# Architecture Extension: Event-Driven And General Application Mutations

## Summary

`cleanCrud` started as a framework for well-structured CRUD applications.
That remains a valid and valuable core identity. But the framework has already
grown beyond "REST CRUD scaffolding":

- it owns explicit application workflows
- it owns transaction boundaries
- it supports post-commit side effects
- it supports durable follow-up processes
- it separates transport from application services

The next logical step is to acknowledge that CRUD is only one family of
application operations.

This document proposes an architectural extension in which `cleanCrud` grows
from a CRUD-focused framework into a broader application operation framework
with strong built-in CRUD support.

The main new capability discussed here is **event-driven and command-driven
mutation support**:

- HTTP user-intent mutations
- application-internal commands
- authoritative external events

all using the same persistence and workflow infrastructure where appropriate,
without forcing every mutation into a single `DomainModelUpdatePatch` shape.

---

## Motivation

The immediate motivation comes from a trading use case.

An `Order` aggregate is:

- created and queried through standard REST CRUD
- mutated both by user-intent requests and by external broker events

Examples:

- user cancels order
- broker acknowledges submission
- broker reports rejection
- broker reports fill

These operations are not naturally representable as one shared patch DTO.

Trying to force them into one `DomainModelUpdatePatch` leads to a
fat optional-field union shape such as:

- `fillPrice?`
- `fillQty?`
- `rejectedAt?`
- `rejectionReason?`
- `cancelledAt?`
- `acknowledgedAt?`

That is structurally weak and semantically dishonest.

The second problem is policy semantics.

User-intent updates and authoritative external events are not the same thing:

- user intent is a request and often must pass permission and policy checks
- external events are facts from a trusted upstream system and often should
  bypass permission checks while still respecting domain invariants

The current update model is too narrow for this distinction.

---

## Broader Observation

The trading example is not a niche exception.

Many real systems have the same shape:

- payment provider webhooks updating payment state
- shipment provider events updating delivery state
- IoT telemetry updating device state
- workflow engine callbacks updating business state
- scheduled internal jobs applying state transitions

In all of these cases, CRUD handles part of the system, but not all of it.

The missing architectural concept is not "better patching". The missing concept
is a broader application operation model.

---

## Proposed Conceptual Model

The framework should think in terms of three parallel operation families:

### 1. Queries

Queries read application state.

Characteristics:

- no side effects
- read-only transaction semantics when needed
- transport-neutral
- existing fetch services already fit this concept well

### 2. Mutations

Mutations change application state inside one business workflow and one
transactional boundary.

Characteristics:

- may start from REST, internal API, message consumer, scheduler, or process
- may represent user intent, internal command, or external fact
- may emit post-commit side effects
- may start durable follow-up processes

CRUD mutations are one specialization of this family:

- create
- replace
- patch
- delete

Command/event mutations are another specialization:

- acknowledge order
- reject order
- apply fill
- cancel order

### 3. Processes

Processes are durable, retryable, potentially long-running follow-up workflows.

Characteristics:

- persisted task state
- retries and backoff
- immediate after-commit nudge plus polling recovery path
- emit follow-up application actions

This concept already exists in the framework as the durable-process subsystem.

---

## What `cleanCrud` Would Become

Under this direction, `cleanCrud` would become:

> a framework for application queries, mutations, and durable processes, with
> first-class CRUD batteries included

That is a stronger and more honest description of the architecture than
"CRUD scaffolding", while still preserving CRUD as the most convenient default.

This is not a rejection of CRUD.

It is an architectural generalization:

- CRUD remains the easiest path for standard aggregates
- more general stateful application mutations become a sibling capability
- durable processes remain a separate but integrated capability

---

## Should This Be A Parallel Path Or Merged Into CRUD?

This must be answered carefully.

### Short answer

It should be **merged at the workflow/runtime substrate level** and **parallel
at the application operation level**.

### What should be shared

The following concerns should be shared:

- workflow execution
- transaction boundary handling
- persistence path
- audit and mutation context construction
- post-commit side effects
- durable process start
- application-level integration points

### What should remain distinct

The following concepts should remain distinct:

- CRUD patch/update semantics
- command/event mutation semantics
- query semantics
- durable process semantics

### Why not force everything into CRUD

If the framework tries to model every mutation as "create/put/patch/delete",
it will:

- distort domain language
- force weak DTO designs
- blur policy meaning
- make non-CRUD use cases look like hacks

### Why not build a completely separate runtime

If command/event mutation support gets its own isolated runtime stack, it will:

- duplicate transaction logic
- duplicate post-commit handling
- duplicate process-start handling
- drift away from CRUD behavior

### Working design conclusion

The right shape is:

- one shared mutation workflow substrate
- multiple mutation families built on top of it

So this is neither "just bolt it into patch CRUD" nor "build a second
framework beside CRUD". It is a controlled generalization.

---

## Architectural Direction

The framework should introduce a first-class **application mutation** concept.

The exact names can be refined later, but the architectural intent is:

- CRUD update is one mutation style
- command-driven mutation is another mutation style
- event-driven mutation is another mutation style

All of them should converge into one mutation workflow model.

Possible conceptual vocabulary:

- `ApplicationMutation`
- `MutationSource`
- `MutationWorkflow`
- `MutationHandler`
- `MutationPolicy`
- `MutationContext`

This would sit alongside:

- query services
- durable processes

---

## Mutation Sources

A central missing concept today is **source-aware mutation semantics**.

Not all mutations should be treated the same way.

At minimum, the framework should eventually distinguish:

- `USER_INTENT`
- `INTERNAL_COMMAND`
- `AUTHORITATIVE_EXTERNAL_EVENT`
- `PROCESS_EMITTED_ACTION`

The source matters because it changes semantics:

- permission checks
- policy checks
- invariant enforcement
- audit metadata
- idempotency expectations
- ordering/staleness rules

This is more robust than adding only a special-case "event update service".

---

## Command And Event Mutation Support

### Why a single patch type is insufficient

A single `DomainModelUpdatePatch` assumes:

- one update shape per aggregate
- one patching strategy
- one policy interpretation

That works for classic REST patching, but it does not work well for:

- sealed command hierarchies
- strongly typed event payloads
- non-overlapping mutation shapes

### Desired model

For non-CRUD mutation families, the framework should support:

- typed command or event payloads
- one handler per payload type or mutation kind
- mutation-specific transformation of the current domain model
- mutation-source-aware policy enforcement

Consumer-side examples may naturally look like:

```java
sealed interface OrderMutation
		permits CancelOrder, AcknowledgeOrder, RejectOrder, ApplyFill
{
}
```

But the framework should not overfit itself to Java sealed-type syntax.
The real requirement is:

- typed mutation payloads
- explicit handler dispatch
- shared mutation workflow execution

### Handler concept

The framework will likely need an abstraction like:

- `MutationHandler<DomainModel, MutationPayload>`

with responsibilities such as:

- validate or interpret the mutation payload
- transform current model into next model
- optionally reject invalid or stale mutations

This should not automatically imply REST exposure.

---

## CRUD As A First-Class Specialization

CRUD should remain a first-class concept, not be reduced to "just another
generic operation".

That means:

- existing save/fetch/update/delete services remain supported
- generators can keep producing CRUD modules
- REST CRUD remains the easiest default path
- current relationship-aware aggregate CRUD remains a flagship feature

However, CRUD should increasingly be seen as:

- a specialized mutation/query surface
- implemented on top of more general workflow primitives

This preserves simplicity for common applications while unlocking richer
architectures for advanced ones.

---

## Event-Driven Support

Event-driven mutation support should be designed as a normal application path,
not as a workaround.

### Desired properties

An event-driven mutation path should:

- be callable from application code directly
- work from async consumers and callback adapters
- use the same persistence and transaction model as normal mutations
- emit the same post-commit hooks
- start the same durable follow-up processes
- build the same audit context where applicable

### Typical runtime shape

The intended flow is:

1. external adapter receives event
2. adapter translates infrastructure payload into domain mutation payload
3. application mutation service applies the mutation
4. framework persists state inside the normal mutation workflow
5. post-commit hooks and durable process starts happen as usual

This keeps the external adapter thin and keeps business semantics inside the
application layer.

### Relationship to durable processes

Event-driven mutation support is not the same feature as durable processes.

They are related but distinct:

- event-driven mutation is an immediate application mutation path
- durable process is a persisted long-running follow-up mechanism

However, they compose naturally:

- a durable process may emit an internal action
- that action may be handled as a mutation
- that mutation may itself emit post-commit work

That is the correct layering.

---

## Policy Model Evolution

The current `PatchPolicy` is too specific for the broader direction.

It fits user-intent patch semantics, but it does not fit the whole mutation
space.

The policy model likely needs to evolve toward something like:

- patch-oriented policies for REST patch behavior
- source-aware mutation policies for broader mutations
- invariant policies that apply regardless of source

A likely long-term split is:

- access/permission policy
- invariant policy
- mutation-family-specific policy

For example:

- user patch may require access check plus patch policy plus invariants
- broker event may bypass access check but still enforce invariants
- internal command may use a different policy lane altogether

This is one of the strongest arguments for generalizing beyond pure CRUD.

---

## Audit And Mutation Context

If non-CRUD mutations become first-class, the mutation context model should
eventually grow as well.

Today the framework already knows:

- mutation kind
- id
- previous model
- current model

Future mutation contexts may also need:

- mutation family
- mutation source
- payload type
- correlation id
- causation id
- external event metadata

This would improve:

- audit trails
- observability
- idempotency support
- debugging of async and event-driven flows

This is not required for the first implementation phase, but the architecture
should leave room for it.

---

## Relationship Aggregates

This area needs explicit caution.

For simple aggregates, command/event mutation support is conceptually easy:

- load current model
- apply typed mutation
- validate
- persist

For aggregates with satellites or relationship reconciliation, the problem is
harder.

Questions that must be answered later:

- can command/event handlers update only the root aggregate
- can they also change relationship state
- if they can, how does reconciliation work
- does the handler return only a new root model or a richer aggregate mutation
  result

Because of this, the likely practical rollout is:

1. first support non-relationship aggregates
2. then design relationship-aware command/event mutation support deliberately

The framework should not pretend this is free.

---

## Transport Neutrality

A central principle should be preserved:

the application mutation model must not be REST-shaped.

REST is one adapter.

Other valid adapters include:

- in-process application API
- message listener
- broker callback adapter
- scheduler
- durable-process runner

If the core design remains transport-neutral, the framework becomes useful for
systems that are not primarily REST systems.

This is important for the future identity of `cleanCrud`.

---

## Vision For The Framework

If this direction is followed carefully, the framework evolves toward:

- clean application service boundaries
- clear workflows for reads, mutations, and processes
- strong transport neutrality
- first-class CRUD
- first-class internal and event-driven mutation support
- durable, retryable long-running follow-ups

That is a much more formidable and future-proof architecture than a framework
limited to request/response CRUD.

It also opens space for future features that fit naturally rather than feeling
bolted on:

- richer internal application API
- source-aware audit trails
- idempotent event handling
- optimistic concurrency support
- query specialization beyond generic fetch
- command/event generation support in templates

---

## What This Document Does Not Propose Yet

This document is architectural, not an implementation spec.

It does not yet lock in:

- exact class names
- exact package names
- exact handler registration mechanism
- exact generator DSL shape
- exact relationship-aggregate behavior
- exact audit metadata model

Those should be decided in a follow-up design once the architectural direction
is accepted.

---

## Recommended Incremental Path

The safest path is evolutionary, not revolutionary.

### Phase 1

Clarify and formalize the architectural vocabulary:

- query
- mutation
- process
- mutation source

### Phase 2

Introduce a first application-internal mutation path for non-relationship
aggregates:

- typed payload
- handler dispatch
- shared workflow execution
- shared post-commit and durable-process behavior

### Phase 3

Add event-oriented semantics:

- authoritative-event mutation source
- differentiated policy behavior
- better mutation context

### Phase 4

Extend the model to relationship-aware aggregates if still desired.

### Phase 5

Consider generator and consumer scaffolding once the core framework shape is
stable.

This avoids prematurely encoding the wrong design into generated templates.

---

## Working Conclusion

The trading use case reveals a real architectural limit in the current
CRUD-only mutation model.

The correct response is not merely "support polymorphic update commands" as a
local feature. The correct response is to recognize a broader architectural
truth:

- CRUD is one important application operation family
- but not the only one

`cleanCrud` should therefore evolve toward a framework for:

- queries
- mutations
- durable processes

with CRUD remaining the most convenient built-in specialization.

Under that architecture:

- REST CRUD remains strong
- command-driven and event-driven mutations become first-class
- durable processes remain separate but integrated
- the framework becomes useful for a wider class of real systems

That is a coherent and ambitious direction, and it aligns with where the
framework is already naturally heading.