# Aggregate Engine Architecture Note

## Summary

The current aggregate CRUD path centralizes too much orchestration inside
`DefaultAggregateLifecycleEngine`.

This is not primarily a functional bug. The concern is architectural:

- use-case orchestration for `save`, `put`, `patch`, and `delete` is no longer
  primarily owned by the corresponding application services
- the aggregate services have become thin forwarding wrappers
- the engine has become the effective owner of transaction boundaries,
  validation sequencing, relationship orchestration, bulk semantics, and now
  post-commit side effects

That shape is hard to justify in a framework that aims to present a clean
architecture model with separated use cases.

This note documents:

- what the problem is
- how the framework used to be structured
- why the current design is less desirable
- what direction a future refactor should likely take
- why the work must be coordinated across downstream consumers

---

## Current Situation

For aggregate CRUD, the runtime path is now:

1. controller/facade calls an aggregate application service
2. the service delegates to `AggregateLifecycleEngine`
3. `DefaultAggregateLifecycleEngine` opens the transaction
4. the engine performs the mutation orchestration
5. the engine calls fetch/mutation ports and relationship coordinators
6. the engine dispatches post-commit work after successful commit

This means the aggregate application services are mostly transport wrappers over
the engine.

For single-entity aggregates without satellites, the code still goes through the
same engine. The only difference is that the engine takes the
`relationships.isEmpty()` branch. There is no separate save/update/delete
orchestration path at the service level for the simple case.

That is important because the problem is not "satellite complexity". The
problem is that use-case orchestration has been centralized into one large
runtime hub.

---

## Why This Is A Problem

### 1. Use-case separation has been weakened

The framework spent significant effort separating:

- save
- update
- delete
- fetch

into distinct application service concepts.

The current aggregate path partially undoes that separation by moving the main
mutation orchestration into one engine class.

As a result:

- `AbstractSaveService` no longer clearly owns save orchestration
- `AbstractUpdateService` no longer clearly owns update orchestration
- `AbstractDeleteService` no longer clearly owns delete orchestration

Instead, these services mostly forward into the engine.

### 2. One class owns too many responsibilities

`DefaultAggregateLifecycleEngine` currently owns or coordinates:

- transaction boundary management
- validation sequencing
- simple mutation flow for aggregates without relationships
- mutation flow for aggregates with relationships
- bulk operation semantics
- dispatch of post-commit side effects

Even if some detailed relationship logic lives in helper coordinators, the
engine is still the main orchestration hub for almost all aggregate mutations.

This is not a clean separation of responsibilities.

### 3. The architecture becomes harder to reason about

The earlier service-centric shape made the orchestration path obvious:

- save logic lived in save service
- update logic lived in update service
- delete logic lived in delete service

Now, reasoning about aggregate mutation behavior requires tracing:

- service
- engine
- transaction runner
- relationship coordinators
- fetch and mutation ports
- post-commit dispatcher

That is a lot of indirection for framework users and maintainers.

### 4. Extension pressure accumulates in the engine

Any new aggregate-wide capability naturally gets added to the engine because it
is now the central point that "knows everything".

Examples:

- post-commit mutation hooks
- future retryable dispatch
- additional mutation policies
- new aggregate mutation semantics

This creates a strong tendency toward a monolithic application-layer class.

---

## How It Was Earlier

Before the aggregate engine-centric design, the framework had a more
use-case-oriented structure.

Using tag `v0.6.0` as the reference point:

- `AbstractSaveService` owned save validation and save orchestration
- `AbstractUpdateService` owned patch/put orchestration
- `AbstractDeleteService` owned delete validation and delete orchestration

The persistence layer was still abstracted behind persistence services and
ports, but the application service remained the obvious owner of the use case.

That older shape had a clearer architectural story:

- one service per use case
- orchestration local to the use case
- persistence behind abstractions
- transaction concerns shared where needed, but not at the cost of collapsing
  use-case ownership

This earlier structure aligns better with the stated goals of clean
architecture.

---

## Important Clarification

This note is **not** arguing for a split between:

- aggregates with satellites
- aggregates without satellites

That split would be the wrong fix.

The framework should still present one coherent aggregate model. A single-entity
aggregate should not take a fundamentally different architectural path than an
aggregate with relationships.

The problem is not that all aggregates share one path.

The problem is that the shared path is owned by a large engine rather than by
well-separated use-case orchestrators.

---

## Better Direction

The better direction is likely:

- keep one aggregate concept
- keep one aggregate-oriented orchestration model
- keep shared relationship helpers
- keep shared transaction handling
- restore use-case ownership of orchestration

In practice, that probably means one of the following designs.

### Option A: Services own orchestration directly

- `AbstractSaveService` orchestrates aggregate save
- `AbstractUpdateService` orchestrates aggregate put/patch
- `AbstractDeleteService` orchestrates aggregate delete
- each receives the collaborators it needs, including transaction runner and
  aggregate helpers

### Option B: Dedicated per-use-case aggregate handlers

- `AggregateSaveHandler`
- `AggregateUpdateHandler`
- `AggregateDeleteHandler`
- services delegate to the corresponding handler, not to one big engine

Under either approach:

- transaction execution can still be shared
- relationship planning/coordinators can still be shared
- post-commit dispatch can still be shared

But the orchestration logic is split by use case instead of being concentrated
in one generic engine.

---

## Role Of The Engine Going Forward

If the engine remains at all, it should probably become much smaller.

A reduced engine could be limited to concerns like:

- transaction execution helper
- shared mutation dispatch helper
- composition root for smaller aggregate handlers

What it should probably **not** continue to be:

- the main owner of save orchestration
- the main owner of update orchestration
- the main owner of delete orchestration
- the central place where every aggregate feature gets added

---

## Why This Cannot Be Fixed In Isolation

This is not a safe local refactor.

The current aggregate structure is already part of the effective contract for:

- this repository
- generated code patterns
- downstream consumers using aggregate CRUD definitions/services
- possibly the generator repository and generated templates

Changing the orchestration shape will likely affect:

- constructor and dependency expectations
- service wiring
- generated code
- test fixtures
- extension points such as post-commit mutation behavior

Because of that, the work must be coordinated across all affected repos and
consumers.

It should be treated as a synchronized architectural change, not as an isolated
cleanup inside `cleanCrud`.

---

## Recommended Delivery Model

This work should be given to a separate agent or workstream with explicit
ownership of the coordinated change.

That work should include:

1. mapping every downstream consumer and generator touchpoint
2. deciding the target architecture before implementation starts
3. updating framework and generator code in sync
4. updating consumer code in the same rollout window
5. verifying that the new shape still supports the aggregate use cases already
   covered today

This is especially important because doing the refactor halfway would likely
leave the framework in a worse transitional state than either the old or the
current design.

---

## Working Conclusion

The current engine-centric aggregate mutation architecture is functional but
architecturally weak.

It centralizes too much use-case orchestration in one place and partially
undermines the framework's earlier, cleaner separation of save/update/delete
application services.

The likely correct long-term move is:

- not to split simple vs complex aggregates
- not to keep adding features into the engine
- but to move aggregate orchestration back into per-use-case handlers or
  services, while retaining shared transaction and relationship helpers

Because that change will ripple into generators and downstream consumers, it
should be planned and executed as a coordinated cross-repo migration.
