# cleanCrud Maintenance And Development Guidelines

## Why This Document Exists

`cleanCrud` has grown from a CRUD scaffolding helper into a layered application
framework with multiple parallel operation lanes, a durable process subsystem,
a mutation quarantine subsystem, and source-aware policy machinery.

It is now large enough that no single person can hold the entire system in their
head at once. This document exists to make that manageable.

It is not a design document. It is a discipline document. It captures the
working practices that keep a large solo-maintained framework coherent over time:
what to do before writing code, what to watch for while writing it, and what
signals mean the structure is drifting.

---

## The Mental Model: Seven Subsystems

The entire codebase is easier to reason about as seven bounded mental modules.
You only need to hold one in your head while working in it. The seams between
them are the ports and the engine interface.

| Subsystem                      | Core responsibility                                                                   | Public seam                                                  |
|--------------------------------|---------------------------------------------------------------------------------------|--------------------------------------------------------------|
| **Operation substrate**        | Transaction boundary, workflow execution, post-commit dispatch, durable process start | `AggregateLifecycleEngine`, `CrudWorkflow`                   |
| **CRUD lane**                  | Create/fetch/update/delete use-case services and relationship orchestration           | `AggregateCrudServices`, `AggregateCrudDefinition`           |
| **Operation umbrella**         | Shared operation domain types, shared quarantine base, two fully symmetric lanes      | `OperationSource`, `AbstractQuarantineService`               |
| **Mutation lane**              | Typed command/event handling, source-aware mutation policy, mutation quarantine       | `AggregateMutationServices`, `MutationApplicationController` |
| **Creation lane**              | Typed creation handling, source-aware creation policy, creation quarantine            | `AggregateCreationServices`, `CreationApplicationController` |
| **Durable process subsystem**  | Persistent follow-up tasks, retry, scheduling, action dispatch                        | `DurableProcessStarter`, `DurableProcessRunner`              |
| **Tri-temporal historization** | Three-axis audit trail, actor capture, temporal conflict detection                    | `TriTemporalHistoryRecorder`, `AuditActorSupplier`           |
| **Domain declarations**        | Relationship model, lifecycle semantics, policy interfaces                            | `Relationship`, `LifecycleSemantics`, policy interfaces      |

The mutation and creation lanes live inside the operation umbrella. They are
siblings, not independent subsystems. Changes to shared operation domain types
(`OperationSource`, `OperationFamily`, `ApplicationOperationPayload`, correlation
and causation IDs) affect both lanes simultaneously.

When adding a feature, identify which subsystem owns it before writing any code.
If a feature genuinely spans two subsystems, that is a signal to define the
seam explicitly rather than letting the implementation create one accidentally.

---

## Rule One: Spec Before Code

No new lane, no new subsystem, no significant extension without a specification
document first.

This is not process for its own sake. Writing the spec forces the seams to
appear on paper before they appear — or fail to appear — in code. A spec that
cannot explain what a feature does *not* do is not finished.

### What a spec document must answer

1. **What problem does this solve?** State it as a consumer need, not as an
   implementation idea. The `letter.md` feature request pattern is the right
   model: write it as if a real consumer is describing a gap they hit.

2. **What is the public API surface?** Name the interfaces and entry points the
   consumer will use. If you cannot name them before implementation, the design
   is not ready.

3. **What does this subsystem explicitly not do?** Every subsystem needs a
   stated boundary. The durable process subsystem already has one:
   "this is not a DAG orchestrator". Every new subsystem needs the equivalent.

4. **Which existing subsystem does this extend or interact with?** Name the
   seams it touches.

5. **What are the default behaviours?** Policy defaults, noop implementations,
   and opt-in configuration should all be decided at spec time, not discovered
   during implementation.

### Where specs live

Feature specifications go in `.specification/`.
Implementation notes and architectural decisions go in
`.specification/implementation/`.
Refactoring plans and known structural debt go in `.refactoring/`.

---

## Rule Two: The Engine Must Not Grow

`DefaultAggregateLifecycleEngine` is the most dangerous class in the codebase.
It sits at the centre of everything: every CRUD operation, every mutation, every
durable process start, every quarantine recording access flows through it.

If it becomes hard to reason about, the entire framework becomes hard to reason
about.

### The current warning sign

As of the creation lane addition, the engine has more than nine static factory
methods. Each new subsystem capability has added another combination. This must
not continue.

### The required fix

Replace all static factory methods with a single builder:

```java
DefaultAggregateLifecycleEngine.builder()
    .

transactionRunner(runner)
    .

durableProcessStarter(starter)
    .

mutationQuarantineRecorder(mutationRecorder)
    .

creationQuarantineRecorder(creationRecorder)
    .

build()
```

Every new subsystem that needs engine integration adds one builder method.
All fields default to their noop variants when not provided. The factory
methods are removed entirely.

This refactor is the single highest-priority structural repair in the codebase.
Do not add further capabilities to the engine before this is done.

### The engine's stated responsibilities

The engine owns exactly these concerns and no others:

- transaction boundary execution via `PersistenceTransactionRunner`
- durable process task start inside the transaction
- post-commit dispatch after the transaction
- immediate durable process execution nudge after commit
- quarantine recorder access for mutation and creation lanes

The engine does **not** own:

- business sequencing for any operation family
- relationship orchestration
- policy evaluation
- handler dispatch

If a new capability does not fit cleanly into one of the five responsibilities
above, it does not belong in the engine.

---

## Rule Three: Parallel Lanes Must Mirror Each Other

The mutation lane and the creation lane are the first two instances of a
pattern that will likely recur. Consistency between them is more valuable than
local optimisation within either.

When a capability exists in one lane, ask whether it belongs in the other
before deciding it does not.

| Capability              | Mutation lane                     | Creation lane                           |
|-------------------------|-----------------------------------|-----------------------------------------|
| Source taxonomy         | `MutationSource`                  | *(needs equivalent)*                    |
| Policy profile          | `MutationPolicyProfile`           | `CreationPolicyProfile`                 |
| Policy profile resolver | `MutationPolicyProfileResolver`   | `CreationPolicyProfileResolver`         |
| Quarantine              | `MutationQuarantineRecorder`      | `CreationQuarantineRecorder`            |
| Quarantine replay       | `MutationQuarantineReplayGateway` | *(needs equivalent)*                    |
| Application controller  | `MutationApplicationController`   | *(needs equivalent)*                    |
| Handler registry        | `MutationHandlerRegistry`         | *(needs equivalent or decided against)* |

Gaps are acceptable if there is a documented reason. Gaps that exist because
the second lane was implemented faster than the first are not acceptable.

### Naming discipline for the operation umbrella

The parent package `useCases.operation` is the right home for both lanes and
any future lanes. The naming pattern within each lane should mirror the other
exactly. A reader who understands the mutation lane should be able to navigate
the creation lane without a guide.

---

## Rule Four: Contract Tests, Not Implementation Tests

For a framework with downstream consumers, the most important tests are those
that verify the consumer-facing contracts — not the internal wiring.

### What a contract test looks like

A contract test wires a concrete aggregate definition, exercises an operation
through the public API, and asserts on the outcome visible to a consumer:

- the returned model state
- the exception type and message on rejection
- the quarantine record on quarantine
- the durable process task on process start

When the internal implementation changes, contract tests survive. When internal
implementation tests are written against private wiring, refactoring breaks them
even when no consumer-facing behaviour changed.

### The sample implementation as the primary contract test suite

The `cleanCrud-sampleImplementation` repository is the de facto contract test
suite. It exercises the framework end-to-end through real aggregate definitions,
real persistence, and real policy configurations.

Every significant feature addition must be represented in the sample
implementation before it is considered complete. A feature that cannot be
demonstrated in the sample has no verified contract.

### The thin test count warning

The current test count is low relative to the framework's surface area. Until
that is addressed, the sample implementation is the primary safety net. Do not
reduce investment in the sample to increase investment in unit tests — they
serve different purposes.

---

## Rule Five: The Architecture Document Is a Pre-Commit Checklist

`00_EXISTING_ARCHITECTURE.md` is the most important single file in the
repository. Its value depends entirely on it being accurate.

The discipline is: update it **before** merging a significant change, not after.

Before committing a change that alters a lane, a subsystem, or the engine, ask:

- Does the architecture document still accurately describe the system?
- Does the cast of characters section still list the right actors?
- Does the story for the affected lane still reflect the real execution path?

If the answer to any of these is no, update the document as part of the change.
A diff that touches `DefaultAggregateLifecycleEngine.java` but not
`00_EXISTING_ARCHITECTURE.md` is a candidate for review.

### The canary test

If writing an accurate update to `00_EXISTING_ARCHITECTURE.md` feels
difficult — if you cannot explain the new feature as a clean story — the feature
is probably not finished from an architectural perspective. The implementation
may compile and pass tests while the design is still incomplete. The architecture
document makes that visible.

---

## Rule Six: Watch the Change Footprint

Each time a feature is implemented, count the files touched. Use that count as
a signal.

### Normal footprint

Adding a new operation handler for an existing aggregate should touch:

- the handler class
- the handler registration
- the configuration class
- the architecture document if the lane changed

Four to six files is a healthy feature footprint.

### Warning footprint

If adding a new capability requires changes in fifteen or more files across four
or more packages, that is a signal that a boundary is in the wrong place. The
right response is to pause and ask which abstraction is missing before continuing
the implementation.

### The specific smell to watch

If the same logical change requires edits to both the engine, the definition
interface, two or more coordinator classes, and multiple configuration files,
the engine is owning too much. That is the god-class accumulation pattern the
`aggregate_engine_architecture_note.md` already diagnosed. Each occurrence of
this smell is evidence that the engine builder refactor (Rule Two) is overdue.

---

## Rule Seven: Respect the Tri-Temporal Historization Boundary

The tri-temporal historization subsystem is one of the most sophisticated parts
of the framework and one of the easiest to accidentally damage. It captures
three independent temporal axes for every aggregate mutation:

- **Transaction time** — when the change was recorded in the database. Set
  automatically by `TriTemporalHistoryModelEntryListener` in `@PrePersist` and
  `@PreUpdate`. This field is immutable after persistence. Application code must
  never set it.
- **Decision time** — when the business decision was made. Can legitimately
  differ from transaction time when recording corrections or backdated events.
- **Validity period** — `validFrom` / `validTo`. Open-ended records use
  `9999-12-31T23:59:59Z` as the sentinel end value, enforced by a database
  constraint (`valid_to >= valid_from`).

### The delete record pattern must not change

The delete recording pattern is deliberately specific and must be preserved
exactly:

1. The current history record is closed: its `validTo` is set to `now()`.
2. A terminal record is created: `validFrom = validTo = now()`, `changeType = DELETED`.

This means a time-travel query to any moment before deletion returns the
correct pre-deletion state. A query at the deletion instant returns the terminal
record. Nothing is erased. This behaviour is the entire point of tri-temporal
modelling. Any change to the delete path that breaks this invariant destroys the
audit trail's correctness.

### The audit actor model is forensic-grade

`AuditActor` captures not just who made a change but how they were
authenticated when they made it:

| Field                                        | Purpose                                             |
|----------------------------------------------|-----------------------------------------------------|
| `actorId`                                    | Required unique identifier                          |
| `actorKind`                                  | `HUMAN`, `SERVICE`, or `SYSTEM`                     |
| `authenticationKind`                         | `JWT`, `SESSION`, `API_KEY`, `BASIC`, or `INTERNAL` |
| `tokenId`, `sessionId`, `issuer`, `clientId` | Forensic authentication provenance                  |

This taxonomy is stable and covers every realistic authentication context.
Do not add new `authenticationKind` or `actorKind` values without considering
the full compliance and query implications for all existing consumers.

### The single consumer integration point

`AuditActorSupplier` is the only place a consumer touches this subsystem.
Everything else is automatic. Consumers provide an actor supplier — typically
sourced from Spring Security's context — and the framework handles the rest.

If a consumer needs richer history queries (time-travel, point-in-time state),
those must be exposed through a domain port, not by reaching directly into
the history infrastructure. The gap noted in `refactoringIdeas.md` section 2.8
— no `AggregateHistoryPort` at the use-case layer — remains open. When it is
addressed, the solution must preserve the subsystem's isolation: the
infrastructure adapter translates `TriTemporalHistoryModel` records into domain
snapshots; the use-case layer never sees JPA types.

### What this subsystem does not do

- It does not expose time-travel queries at the domain or use-case layer yet.
  That is a known gap, tracked in `refactoringIdeas.md` 2.8.
- It does not support correction of past decision times or validity periods
  after the fact (bi-temporal correction). Records are append-only.
- It does not aggregate history across multiple aggregates. Each aggregate's
  history is self-contained.
- It does not replace a dedicated audit log service for compliance reporting.
  It is the raw temporal store; presentation and compliance querying are
  consumer responsibilities.

---

## Rule Nine: State What Each Subsystem Does Not Do

Every subsystem needs an explicit non-responsibility statement. These are as
important as the responsibility statements.

Current non-responsibilities that should be documented:

| Subsystem                  | Does not do                                                                                                               |
|----------------------------|---------------------------------------------------------------------------------------------------------------------------|
| Durable process            | DAG/workflow orchestration, multi-node claiming, distributed coordination                                                 |
| Mutation quarantine        | Business logic — it delegates replay back to the aggregate mutation service                                               |
| Creation quarantine        | Same boundary as mutation quarantine                                                                                      |
| Relationship orchestration | Many-to-many relationships (resolved at domain level via join aggregates)                                                 |
| Operation substrate        | Business sequencing — that belongs to the lane services                                                                   |
| Tri-temporal historization | Time-travel queries at use-case layer, bi-temporal correction, cross-aggregate history, compliance reporting presentation |

When a new feature request arrives, check whether it belongs inside an existing
subsystem or whether accepting it would violate a stated non-responsibility.
The non-responsibility statements are the gatekeeping mechanism.

---

## Rule Ten: Changelog Discipline

A single CHANGELOG becomes unreadable at this scale. Maintain a short changelog
entry per subsystem in `.refactoring/` or a dedicated `CHANGES/` directory.

Each entry answers:

- what changed
- which version introduced it
- whether any consumer-facing contracts changed
- what migration is required if contracts changed

This does not need to be exhaustive. It needs to be accurate enough that a
consumer upgrading between versions can identify what they need to review.

---

## Warning Signs At A Glance

These are the observable signals that the architecture is drifting:

| Signal                                                                           | What it means                                                   |
|----------------------------------------------------------------------------------|-----------------------------------------------------------------|
| `DefaultAggregateLifecycleEngine` gains a new static factory method              | Engine builder refactor is overdue                              |
| A lane or subsystem cannot be described as a clean story                         | Design is incomplete — stop and write the spec                  |
| `00_EXISTING_ARCHITECTURE.md` is out of date                                     | A change was merged without completing it                       |
| A feature touches 15+ files                                                      | An abstraction boundary is in the wrong place                   |
| One lane has a capability the parallel lane is missing with no documented reason | Lane symmetry is drifting                                       |
| Tests pass but the sample implementation cannot demonstrate the feature          | The consumer contract is unverified                             |
| Writing a new spec feels redundant because "it's obvious"                        | The spec is most necessary when it feels most unnecessary       |
| A persistence repository change does not update the history recorder path        | The delete terminal-record invariant may be broken              |
| Transaction time is set by application code rather than the JPA listener         | The immutability of transaction time has been violated          |
| A new `authenticationKind` or `actorKind` is added without a compliance review   | The forensic audit model is drifting without full consideration |

---

## The Broader Principle

The framework is at a stage where the conceptual architecture is cleaner than
the implementation fully expresses. The `.refactoring/` notes document the gap
accurately.

The most important single discipline is not letting that gap widen faster than
it closes. Every new lane or capability added on top of unresolved structural
debt makes the underlying repair harder. The engine builder refactor, the
contract test suite, and the lane symmetry between mutation and creation are the
three structural repairs that must stay ahead of new feature work.

The framework is impressive because it was built with genuine architectural
thought. It stays impressive by protecting that thought against the entropy that
accumulates in every growing codebase.