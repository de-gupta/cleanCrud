# Implementation Plan: Event-Driven And General Application Mutations

## Purpose

This document turns the architectural direction from
`01_ARCHITECTURE_EXTENSION_EVENT.md` into an implementation roadmap.

It is intentionally more concrete than the architecture document, but it is
still not final API law. The purpose is to preserve the design decisions and
constraints already known at this stage so implementation does not drift into
the wrong shape.

In particular, this document protects against the following failure modes:

- forcing command/event mutations through `DomainModelUpdatePatch`
- creating a second isolated runtime beside CRUD
- prematurely over-generalizing into a vague `Operation<I, O>` framework
- trying to solve relationship aggregates in the first pass
- baking unstable design into the generator before the framework shape is
  proven

---

## Current Baseline

Before this work starts, `cleanCrud` already has important machinery that this
feature must reuse rather than duplicate:

- workflow-based CRUD execution
- `DefaultAggregateLifecycleEngine` as workflow executor
- explicit read-only vs write workflow execution
- `PostCommitMutation`
- durable process start support in CRUD services
- durable process persistence, immediate after-commit nudge, and polling retry

This matters because the new feature is not a greenfield subsystem.
It is an extension of an already richer framework.

The correct implementation path is therefore:

- preserve the shared workflow substrate
- add a new mutation family on top
- reuse post-commit and durable-process behavior

not:

- duplicate CRUD logic into a separate event subsystem
- or distort CRUD update into carrying all mutation semantics

---

## Scope Of This Implementation Line

The implementation line described here is about:

- application-internal mutations
- typed command/event payloads
- source-aware mutation semantics
- reuse of the normal persistence and post-commit path

It is **not** about:

- singleton aggregate support
- generator support in the first pass
- new REST exposure in the first pass
- distributed eventing infrastructure
- outbox implementation
- multi-node coordination
- relationship-aware command/event mutation in the first pass

Those may come later, but they must not distort the initial delivery.

---

## Working Design Constraints

These constraints should be treated as implementation guidance, not optional
preferences.

### 1. CRUD remains first-class

The new feature must not degrade existing CRUD ergonomics.

`save`, `fetch`, `update`, and `delete` remain supported and idiomatic.

### 2. Internal mutation is parallel to CRUD, not a hack inside patching

Do not add ad hoc exceptions into `PatchPolicy` or `AbstractUpdateService`
until "event support" sort of works.

That would create a bad local maximum.

### 3. Shared workflow/runtime, separate application semantics

The mutation family should share:

- workflow execution
- transaction execution
- persistence path
- post-commit dispatch
- durable process start

But it should keep distinct:

- payload shape
- handler dispatch
- policy lane
- mutation source semantics

### 4. Non-relationship aggregates first

Relationship aggregates are a separate design problem.

First pass support should target the root aggregate model only.

### 5. No generator changes until the core framework API stabilizes

The generator should follow the framework, not drive it.

The framework shape must be proven by:

- direct framework tests
- one handwritten sample implementation

before generator templates are touched.

### 6. Do not overfit to sealed interfaces

Sealed command hierarchies are a strong consumer pattern, but the framework
should support typed mutation payloads generally rather than encoding itself
around one Java syntax style.

---

## Recommended Package Direction

The naming can still evolve, but the package split should reflect the real
architecture.

Likely feature root:

- `de.gupta.clean.crud.template.useCases.mutation`

Likely subpackages:

- `...mutation.api.application`
- `...mutation.application.service`
- `...mutation.application.dispatch`
- `...mutation.domain.model`
- `...mutation.domain.policy`
- `...mutation.domain.handler`
- `...mutation.infrastructure` only if infrastructure appears later

The important point is conceptual separation:

- CRUD packages remain CRUD packages
- mutation packages hold the broader non-CRUD mutation model
- both sit beside query and process, not inside each other

---

## Phase 1: Vocabulary And Core Contracts

### Goal

Introduce the minimum vocabulary needed to express non-CRUD mutations without
yet wiring full runtime behavior.

### Deliverables

Add first-class contracts for concepts such as:

- mutation payload
- mutation source
- mutation handler
- mutation request
- mutation result/context

Possible candidate types:

- `MutationSource`
- `ApplicationMutationPayload`
- `MutationHandler<DomainModel, Payload>`
- `MutationRequest<DomainId, Payload>`
- `MutationExecutionResult<DomainId, DomainModel>`

### Design intent

This phase exists to make the model explicit before implementation pressure
pushes us into accidental design.

### Important reasoning

- `PatchPolicy` is too narrow; do not reuse it as the central abstraction
- source semantics must be explicit from the start
- payload and handler are more important than transport in this model

### Milestone

The framework can express the new mutation family in type form, even if no
service executes it yet.

---

## Phase 2: Internal Mutation Service For Simple Aggregates

### Goal

Add a real application service path for typed non-CRUD mutations on
non-relationship aggregates.

### Deliverables

Introduce a service that:

- loads current aggregate by id
- dispatches typed payload to the correct handler
- obtains next domain model
- persists through the normal mutation port
- emits the normal post-commit behavior
- starts durable processes through the same mechanism CRUD now uses

This service should run through the existing workflow engine.

### Likely shape

There will probably be:

- one mutation definition/configuration object per aggregate
- one registry or dispatch mechanism from payload type to handler
- one application-facing service entrypoint

### Important reasoning

This phase proves the architectural thesis:

- non-CRUD mutation is not a separate runtime
- it is a sibling application use case on the same substrate

### Non-goals

- no REST controller generation
- no relationship reconciliation
- no audit redesign yet

### Milestone

A handwritten sample aggregate can be mutated from inside the application using
a typed payload instead of `DomainModelUpdatePatch`, while still reusing the
same persistence, post-commit, and durable-process path.

---

## Phase 3: Policy Split And Source Semantics

### Goal

Make policy behavior explicit for different mutation sources.

### Deliverables

Introduce a more honest policy model, likely separating:

- access/permission concerns
- invariant concerns
- mutation-family-specific concerns

Possible directions:

- `MutationAccessPolicy`
- `MutationInvariantPolicy`
- `SourceAwareMutationPolicy`

The exact names matter less than the separation.

### Important reasoning

The trading use case is not just about multiple payloads.
It is about different truth semantics:

- user patch is a request
- broker event is an authoritative fact

That difference must be visible in policy behavior, not buried in consumer code.

### Implementation caution

Do not break existing CRUD patch behavior during this phase.

Existing CRUD update should continue to behave exactly as before while the new
mutation lane gains richer source semantics.

### Milestone

At least two sources behave differently in tests:

- `USER_INTENT`
- `AUTHORITATIVE_EXTERNAL_EVENT`

with shared runtime path but differentiated validation semantics.

---

## Phase 4: Mutation Context And Observability Enrichment

### Goal

Enrich the mutation result/context model so non-CRUD mutations are auditable and
composable.

### Deliverables

Extend mutation context with room for:

- mutation family
- mutation source
- payload type
- correlation id
- causation id

This may initially be additive and optional rather than fully enforced.

### Important reasoning

Without richer context, the framework may function but remain opaque when used
from async consumers, callbacks, or processes.

This phase is especially important if durable processes or external adapters are
expected to emit mutations reliably.

### Milestone

Post-commit and process-start paths can observe the origin and family of the
mutation they are reacting to.

---

## Phase 5: Event-Consumer Friendly Application API

### Goal

Make the new mutation family pleasant to call from adapters such as broker
callbacks, schedulers, or message consumers.

### Deliverables

Provide a stable application-facing API that lets adapter code do something
like:

1. translate infrastructure event to domain mutation payload
2. call framework application mutation service

without touching transport or CRUD-specific concerns.

### Important reasoning

This phase is where the feature becomes useful in real systems.

The adapter should remain thin.
The framework application layer should own mutation semantics.

### Non-goals

- still no automatic REST exposure required
- still no generator work required

### Milestone

A sample integration adapter can apply an authoritative external mutation using
a concise application API.

---

## Phase 6: Handwritten Sample Validation

### Goal

Prove the shape in the handwritten sample implementation before touching the
generator.

### Deliverables

Add one non-trivial handwritten example, ideally close to the trading pattern:

- one aggregate with standard CRUD
- one or more typed internal/event mutations
- at least one source-aware policy distinction
- post-commit and durable-process compatibility where useful

### Suggested sample characteristics

The sample should prove:

- CRUD and non-CRUD mutation coexist
- the new path is not REST-shaped internally
- adapters remain thin

### Important reasoning

The sample is where awkwardness becomes obvious.
If the handwritten sample feels contorted, the framework API is not ready for
generation.

### Milestone

The sample implementation compiles, tests pass, and the new mutation path feels
credible without template help.

---

## Phase 7: Generator And Consumer DSL

### Goal

Only after the framework shape is stable, add generator support and consumer DSL
extensions.

### Deliverables

Potential future additions:

- generation-spec flags for internal mutation families
- stub handler generation
- optional adapter scaffolding
- optional application API exposure

### Important reasoning

The generator is a force multiplier for both good and bad design.
If the framework API is still moving, generated templates will fossilize the
wrong choices.

### Milestone

Generator changes become mostly mechanical because the underlying framework
shape is already settled.

---

## Deferred Phase: Relationship-Aware Mutation Support

This phase is intentionally deferred and should not be smuggled into earlier
milestones.

### Why it is deferred

The current relationship model is strongly CRUD-oriented.
Command/event mutation introduces harder questions:

- does a handler return only a new root model
- or does it return a richer aggregate mutation result
- how are satellites updated
- how is reconciliation triggered
- how are invariants enforced across the aggregate graph

These are not small details.

### Recommendation

Do not attempt this until the simple-aggregate mutation lane is solid and the
sample has proven the core design.

---

## Testing Strategy

Testing should evolve with the phases.

### Framework tests

Add focused tests for:

- handler dispatch
- workflow execution through the existing engine
- policy differentiation by mutation source
- post-commit dispatch reuse
- durable-process start reuse
- non-relationship mutation persistence path

### Sample tests

Add end-to-end sample tests for:

- internal mutation application
- authoritative event mutation application
- compatibility with ordinary CRUD

### What not to do

Do not rely on generator smoke tests as the first proof of correctness.

---

## Decision Log To Preserve

The following decisions have effectively already been made and should not be
re-litigated during implementation unless there is a strong reason:

- the feature is broader than the original trading letter
- the framework is moving toward query/mutation/process, not remaining CRUD-only
- CRUD stays first-class
- the new mutation lane should be parallel to CRUD, not stuffed into patching
- the runtime substrate should be shared
- non-relationship aggregates come first
- durable processes are separate from immediate event-driven mutation
- generator work comes after framework and handwritten sample stabilization
- singleton aggregate support is a separate feature and not part of this line

Preserving these decisions reduces churn and prevents accidental scope mixing.

---

## Recommended First Actual Coding Slice

When implementation starts, the first coding slice should be deliberately small:

1. add the new mutation vocabulary and contracts
2. implement a simple internal mutation service for non-relationship aggregates
3. wire it through the existing workflow engine
4. prove one handwritten sample aggregate using it

This gives the highest information yield with the lowest design risk.

---

## Working Conclusion

This feature should be implemented as a controlled architectural extension, not
as a local patch to CRUD update handling.

The implementation sequence matters:

- shared vocabulary first
- simple internal mutation path next
- source-aware policy after that
- sample validation before generator work
- relationship support later if still justified

If the sequence is respected, `cleanCrud` can grow into a framework for
queries, mutations, and durable processes without losing the clarity and
ergonomics of its CRUD core.
