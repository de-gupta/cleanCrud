# Durable Post-Commit Process Note

## Summary

The current `postCommitMutation(...)` feature is intentionally lightweight:

- aggregate CRUD commits first
- a `PostCommitMutationContext` is built
- `DefaultAggregateLifecycleEngine` dispatches the hook asynchronously on a
  virtual thread
- failures are logged and forgotten

That is a good default for best-effort side effects such as logging,
notifications, or fire-and-forget downstream publication.

It is the wrong place to grow a richer feature for durable external workflows.

The motivating use case here is not "call a webhook after commit". The
motivating use case is:

- persist an aggregate
- start a durable external submission process
- retry with policy
- interpret the external response
- emit a follow-up internal application event or command
- mutate domain state again based on that outcome
- survive crashes and restarts while doing all of the above

That is a materially different architectural concern. It deserves a separate
feature with separate abstractions and a separate implementation model.

This note documents:

- the concrete broker/trading example that motivated the feature
- what the current post-commit hook does and why extending it is a mistake
- the architectural direction that fits this kind of workflow better
- proposed concepts and implementation ideas for `cleanCrud`
- principles and guardrails so the feature does not collapse into a hidden
  ad-hoc workflow engine

---

## Motivation

The user story that triggered this discussion is a trading engine.

### Trading example

The simplified flow is:

1. a strategy decides to submit an order
2. the order is persisted locally with status `SUBMISSION_REQUESTED`
3. the application sends a submission request to a broker
4. the broker usually sends an immediate acknowledgement or rejection
5. later, the broker sends a separate confirmation event

From the application's point of view, those steps are not just external side
effects. They are part of the business process.

The desired domain state changes are:

1. create order
    - status becomes `SUBMISSION_REQUESTED`
2. broker acknowledges immediate receipt
    - status becomes `SUBMISSION_ACKNOWLEDGED`
3. broker later confirms execution/submission completion
    - status becomes `SUBMISSION_CONFIRMED`
4. broker rejects outright
    - status becomes `REJECTED`

There is also a retry concern:

- if broker submission fails transiently, the application should retry with a
  policy
- if retries are exhausted, the process should end in an explicit failure state
- this behavior must survive process restarts

That means the feature is not simply:

- "after commit, call broker"

It is:

- "after commit, start a durable application process that happens to call a
  broker as one step"

That distinction matters.

---

## What The Current Post-Commit Hook Is

Today the aggregate definition API has:

- `postCommitMutation(PostCommitMutation<DomainId, DomainModel>)`

The hook type is currently just:

- `Consumer<PostCommitMutationContext<DomainId, DomainModel>>`

The context carries:

- `PostCommitMutationKind kind`
- `DomainId domainId`
- `Optional<DomainModel> currentModel`
- `Optional<DomainModel> previousModel`

The runtime behavior is currently:

1. service completes the transactional workflow
2. engine commits the transaction
3. `DefaultAggregateLifecycleEngine` schedules `afterTransaction(...)`
4. `PostCommitMutationDispatcher` runs it asynchronously on a virtual thread
5. runtime exceptions are logged
6. the main CRUD call is already complete and unaffected

This is a strong default for:

- logging
- analytics
- metrics
- best-effort notifications
- non-critical downstream publication

This is intentionally weak for:

- retries across restart
- tracking attempt state
- backoff scheduling
- idempotent delivery
- correlation with downstream acknowledgements
- business-state changes that depend on external outcomes

The current hook is therefore a useful feature, but a small one.

---

## Why Extending `postCommitMutation(...)` Is The Wrong Move

At first glance, it is tempting to evolve the current hook into something with:

- retry attempts
- backoff policy
- sync vs async execution
- failure classification
- fallback actions

That would be a mistake if the target use case is the trading/broker flow.

### 1. The abstraction is too small

`PostCommitMutation` is a plain consumer over a CRUD mutation context. It has
no concept of:

- durable process identity
- correlation id
- retry state
- next-attempt scheduling
- outcome classification
- emitted follow-up command/event
- process status

Adding configuration around that consumer does not change the fact that the
underlying abstraction is just "run this callback after commit".

### 2. The responsibilities are different

The current hook is about:

- post-transaction side effects

The new feature is about:

- durable process initiation
- external interaction
- policy-driven retries
- follow-up application actions
- persisted operational state

Those do not have the same reason to change.

### 3. The failure model is different

The current hook is explicitly best effort.

The new feature needs:

- durable retries
- visibility into failure
- operational state transitions
- defined behavior across restart and crash

That is not "best effort plus more options". It is a different reliability
contract.

### 4. The new feature should not mutate repositories directly

If a durable post-commit process receives an acknowledgement, the clean shape is
not:

- hook directly writes to the database however it wants

The cleaner shape is:

- process outcome becomes an internal application event or command
- the normal application API handles that event/command
- the aggregate is updated through normal business paths

That preserves business rules, auditability, and architectural clarity.

### 5. The result would become a confusing mixed API

If `postCommitMutation(...)` grows into a fully configurable, durable,
stateful external process mechanism, the API stops being obvious.

Consumers would be unable to tell whether they are configuring:

- a tiny side-effect hook
- or a durable application process

Those should be distinct concepts.

---

## The Better Framing

This should be designed as a **durable process** feature, not as "post-commit
hook V2".

The closest existing architectural families are:

- transactional outbox
- process manager
- saga
- command/event driven workflow

For `cleanCrud`, the best near-term fit appears to be:

- **transactional outbox + process manager**

Not:

- a fully generic workflow engine

That gives us enough power without jumping straight into a large orchestration
framework.

---

## Proposed Mental Model

The clean end-to-end picture should be:

1. a CRUD or application command transaction commits
2. during that same transaction, the application persists one or more
   **process tasks** or **outbox records**
3. a background dispatcher picks up due process tasks
4. the dispatcher performs the external integration step
5. the external outcome is mapped to an internal application event or command
6. that event or command is routed through the normal application API
7. aggregate state is updated through regular business handlers
8. the durable process state is updated accordingly

That preserves a clean separation:

- aggregate CRUD/application services own domain mutation
- process subsystem owns durable follow-up execution
- external integrations are adapters

---

## Detailed Broker Example

### Step 1: Strategy submits order

An application command such as `CreateOrder` or `RequestOrderSubmission` is
handled.

Inside one transaction the application:

- creates the order aggregate
- sets status to `SUBMISSION_REQUESTED`
- persists a durable process task such as `SubmitOrderToBroker`

Both the order and the process task must commit together.

If the transaction rolls back:

- there is no order
- there is no process task

This is the outbox/process-start atomicity requirement.

### Step 2: Durable process runner picks task

A background dispatcher finds due tasks of type `SubmitOrderToBroker`.

The process task should contain enough information to submit safely, for
example:

- process task id
- process type
- aggregate id / order id
- correlation id
- serialized submission payload
- current process status
- attempt count
- next-attempt timestamp
- last failure summary

### Step 3: Broker immediate outcome

The broker can respond with:

- acknowledged
- rejected
- transient failure
- timeout / no response

The process layer should not directly patch the order through repository calls.

Instead it should emit a follow-up internal application event or command such as:

- `OrderSubmissionAcknowledged(orderId, correlationId, brokerReference)`
- `OrderSubmissionRejected(orderId, correlationId, reason)`
- `OrderSubmissionRetryScheduled(orderId, correlationId, nextAttemptAt)`
- `OrderSubmissionFailedPermanently(orderId, correlationId, reason)`

Those internal events/commands can then be handled by the normal application
API, which updates the order aggregate:

- `SUBMISSION_REQUESTED` -> `SUBMISSION_ACKNOWLEDGED`
- `SUBMISSION_REQUESTED` -> `REJECTED`
- or some explicit retry/failure state if desired

### Step 4: Later broker confirmation event

Separately, the broker may publish a later external confirmation.

That external callback should enter the system through an adapter, then become
an internal application event or command such as:

- `OrderSubmissionConfirmed(orderId, brokerReference, ...)`

Again, the normal application API handles it and updates the aggregate to:

- `SUBMISSION_CONFIRMED`

### Why this shape is better

This approach keeps responsibilities separated:

- external integration code talks to the broker
- durable process code owns retry/delivery state
- application handlers own business state transitions

It avoids hidden domain mutations inside a side-effect callback.

---

## Principles

### 1. Keep simple post-commit side effects simple

The existing `postCommitMutation(...)` feature should remain the lightweight
best-effort mechanism.

It should continue to be good for:

- logging
- notifications
- simple publication

It should not become the general durable workflow API.

### 2. Domain mutations should still go through the application API

A durable process may eventually cause a domain change, but that change should
normally happen by routing a follow-up internal command/event through the
application layer.

The process subsystem should not become a shadow mutation path.

### 3. Durability must be explicit

If the feature claims retry/backoff/crash safety, its operational state must be
persisted explicitly.

In-memory retry queues are not enough.

### 4. Outcomes must be classified explicitly

Do not hide business behavior inside `catch` blocks and ad-hoc booleans.

The process execution model should classify outcomes such as:

- success/acknowledged
- rejected/terminal business failure
- transient technical failure
- permanent technical failure

### 5. Idempotency and correlation are first-class concerns

Any external submission process needs:

- correlation ids
- deduplication/idempotency strategy
- safe retry semantics

This cannot be bolted on at the very end.

### 6. Avoid becoming a giant workflow engine too early

The target is not a generic BPM/workflow product.

The target is a clean, durable application-process layer for common
post-transaction follow-up flows.

---

## Suggested Feature Boundary

The feature should likely be introduced as a new subsystem with concepts such
as:

- `ApplicationEvent`
- `ApplicationCommand`
- `ProcessDefinition`
- `ProcessTask` or `ProcessInstance`
- `ProcessDispatcher`
- `ProcessExecutor`
- `ProcessOutcome`
- `RetryPolicy`
- `BackoffPolicy`
- `OutcomeMapper`

The exact naming can change, but the separation should remain.

The key point is:

- **simple post-commit hook**
    - keep existing API
    - best effort
    - non-durable

- **durable post-commit process**
    - new API
    - durable
    - tracked
    - retry-aware
    - outcome-aware
    - routes follow-up actions through the application layer

---

## Candidate Architecture

### 1. Application event emission

After a successful CRUD/application mutation, the framework can emit internal
application events.

Those events may be:

- immediate in-memory events for local synchronous handling
- or persisted events/tasks for durable deferred handling

For this feature, the durable path is the important one.

### 2. Durable process start

Within the same transaction that changes the aggregate, persist a process task.

Examples:

- `SubmitOrderToBrokerTask`
- `PublishSettlementRequestTask`
- `ReserveInventoryWithSupplierTask`

This is the actual atomic start point of the process.

### 3. Process task persistence model

A first cut of a persistence model may include:

- task id
- process type
- trigger event type
- aggregate type
- aggregate id
- correlation id
- payload
- status
- attempt count
- next attempt time
- created at
- updated at
- last error
- last outcome code

Potential statuses:

- `PENDING`
- `RUNNING`
- `WAITING_RETRY`
- `SUCCEEDED`
- `FAILED`
- `CANCELLED`

### 4. Process execution contract

A process executor should take a task and return a structured outcome, not just
throw exceptions.

For example:

- `ProcessOutcome.succeeded(...)`
- `ProcessOutcome.retryAt(...)`
- `ProcessOutcome.rejected(...)`
- `ProcessOutcome.failed(...)`

That keeps retry/failure behavior declarative and inspectable.

### 5. Outcome to application action mapping

The most important architectural rule is:

- external outcome -> internal event/command -> normal application handling

Not:

- external outcome -> direct repository patching from background code

That means the process layer should likely emit objects like:

- `ApplicationCommand`
- `ApplicationEvent`

which the application layer can route.

### 6. External event ingress

Later asynchronous callbacks from external systems should follow the same idea:

- adapter receives external message
- adapter translates to internal application event/command
- application layer handles it

This keeps inbound and outbound process behavior symmetric.

---

## Why This May Evolve `cleanCrud` Beyond Pure CRUD

This feature naturally expands the framework from:

- CRUD + REST

toward:

- application API
- internal commands/events
- durable process orchestration
- multiple adapters

That is not a problem if the boundaries stay clear.

REST should become just one adapter among others:

- REST controller
- broker callback adapter
- scheduled dispatcher
- message queue consumer

The framework can still keep CRUD as the default happy path while supporting
richer application architecture on top.

---

## Implementation Ideas

### Option A: Minimal durable outbox first

Start with:

- persisted process task/outbox record
- scheduler/runner
- retry policy
- simple outcome mapping to application commands/events

This gives most of the value without introducing a large general process model.

This is the recommended first implementation target.

### Option B: Full process-definition model immediately

Introduce:

- process definitions
- typed process steps
- resumable multi-stage workflows
- richer state machines

This may eventually be useful, but it is probably too large as the first cut.

Recommendation:

- do not start here

### Option C: Extend `postCommitMutation(...)`

Do not do this for the durable workflow use case.

It would conflate:

- fire-and-forget side effects
- durable external process orchestration

and make both APIs less clear.

---

## Suggested First-Cut Scope

The first real version of this feature should likely support:

1. start a durable process task in the same transaction as an aggregate change
2. poll/find due tasks
3. execute a task through a typed process executor
4. record attempts and scheduling metadata
5. support retry with backoff
6. map outcomes to internal application commands/events
7. route those commands/events through the normal application API
8. mark task lifecycle state accordingly

What it does **not** need initially:

- arbitrary visual workflow graphs
- BPM-style workflow authoring
- fully generic compensation language
- distributed orchestration across many bounded contexts

---

## Open Design Questions

These questions should be settled before implementation begins:

### 1. Naming

Should the subsystem be described as:

- durable process
- process manager
- outbox process
- application workflow

The name matters because it shapes how consumers think about it.

### 2. Event/command model

Does `cleanCrud` want to introduce explicit framework-level:

- `ApplicationEvent`
- `ApplicationCommand`

types now?

Or should the first version keep outcome routing more local and application
specific?

### 3. Process scope

Should durable processes be:

- aggregate-triggered only

or also:

- startable independently of CRUD mutations

The first version can probably start with aggregate-triggered processes only.

### 4. Delivery model

Should execution be:

- polling-based
- event-driven
- or pluggable

Polling is the simplest first cut and probably sufficient initially.

### 5. Failure semantics

When retries are exhausted, should the process emit:

- a terminal failure event
- a manual-intervention-required event
- both

### 6. Idempotency contract

How much idempotency support belongs in framework primitives versus consumer
code?

---

## Recommended Direction

The recommended direction is:

1. keep `postCommitMutation(...)` as the small best-effort hook it already is
2. introduce a separate durable process subsystem
3. use transactional outbox semantics for starting processes
4. route external outcomes back into the normal application API
5. avoid direct repository mutation from process executors where possible
6. start with a minimal durable outbox/process manager model, not a full generic
   workflow engine

That gives `cleanCrud` a path to support richer application workflows without
corrupting the semantics of its existing post-commit hook.

---

## Agent Implementation Guidance

If another agent picks up this feature later, they should start with these
constraints:

- do not extend `PostCommitMutation` into the durable process API
- preserve current post-commit behavior as the lightweight default
- model durable process state explicitly in persistence
- ensure process start is committed atomically with the triggering aggregate
- route follow-up domain changes through application commands/events, not hidden
  repository writes
- bias toward a minimal outbox/process-manager implementation first
- keep transport adapters separate from process orchestration

The first implementation spike should probably focus on a single concrete
vertical slice:

- order created with `SUBMISSION_REQUESTED`
- durable `SubmitOrderToBroker` task created in same transaction
- runner retries with backoff
- broker `ACK` produces internal `OrderSubmissionAcknowledged`
- application API updates order to `SUBMISSION_ACKNOWLEDGED`

If that slice is clean, the abstraction is probably on the right track.