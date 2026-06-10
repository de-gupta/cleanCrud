# Operation Lane Refactoring: Toward One Generic Operation Substrate

## Why This Document Exists

The operation lane (creation + mutation) works, is well-tested, and now has a
crisp public shape (see `operatioLaneShortcomings.md`, items 1–7, all resolved).
But it is **not yet crisp on the inside**. It is two nearly-complete vertical
copies of each other.

This document diagnoses the duplication concretely, contrasts it with the way
the CRUD lane stays generic, and proposes a sequenced refactoring that makes the
operation lane *general* (one mechanism, many operations) instead of *parallel*
(one mechanism per operation, copy-pasted).

This matters **now, before the generator targets the operation lane**. A
generator that emits against two parallel copies will hard-code that duplication
into every consumer project forever. We want the generator to emit against one
generic substrate, the way it already does for CRUD.

---

## The Headline Diagnosis

> The operation lane has **two** operations but **~136 files in two parallel
> copies** and only **~15 shared files**. The CRUD lane has **four** operations
> but shares a **~51-file aggregate substrate** and gives each operation only
> **~12 files of its own**.

The operation lane is *more duplicated with fewer operations*. That is exactly
backwards from where the CRUD lane sits, and it is the core waste.

### File census (operation lane)

| Area                      | Files | Shared?                          |
|---------------------------|-------|----------------------------------|
| `operation/creation/**`   | 66    | No — full vertical slice         |
| `operation/mutation/**`   | 70    | No — full vertical slice         |
| `operation/domain/**`     | 9     | Yes — shared vocabulary          |
| `operation/quarantine/**` | 6     | Yes — extracted in last refactor |

136 of ~151 files live in two parallel copies. The two copies differ, file by
file, mostly by a type parameter and a name prefix (`Creation*` vs `Mutation*`).

### The measuring stick: how the CRUD lane stays crisp

The CRUD lane has four operations (save/fetch/update/delete) yet does **not**
have four parallel copies of everything. It shares:

- **one** `AggregateCrudDefinition` (the domain contract every operation queries)
- **one** `AggregateLifecycleEngine` (dumb transaction + dispatch executor)
- **one** `CrudWorkflow` / `CrudWorkflowBuilder` (the work-shape DSL)
- **one** set of ports (`AggregateFetchPort`, `AggregateMutationPort`)

Each operation contributes only:

- a thin `Abstract*Service` that builds a workflow
- a focused `Aggregate*Coordinator` that owns *that operation's* relationship
  choreography

> The CRUD engine is **dumb** (transaction + dispatch), the definition is
> **smart** (domain knowledge), the coordinator is **focused** (one operation's
> orchestration). Nothing about save/fetch/update/delete is copy-pasted.

The operation lane should reach the same shape: **one** operation substrate,
with creation and mutation as thin specializations — not two substrates.

---

## Answering The Two Questions Directly

### Q1: "Why is there `CreationQuarantineApplicationController` *and*

`MutationQuarantineApplicationController` instead of one generic controller that
takes a parameter?"

Because the quarantine subsystem was built as part of each vertical slice, not as
a shared capability. There is no good reason for it to be duplicated. The two
controllers are **structurally identical** — same four methods, same
access-policy gate, same delegation. They differ only in the record type and the
id type:

```java
// AbstractCreationQuarantineApplicationController        // AbstractMutationQuarantineApplicationController
public CreationQuarantineRecord dismiss(CreationQuarantineId id)
{
	public MutationQuarantineRecord dismiss (MutationQuarantineId id){
	var record = findById(id);
	var record = findById(id);
	accessPolicy.validateDismiss(record);
	accessPolicy.validateDismiss(record);
	return service.dismiss(id);
	return service.dismiss(id);
}
}
```

These should be **one** generic controller parameterized by `<Id, Record>`. This
is Proposal A below, and it is the single highest-leverage, lowest-risk change.

The quarantine subsystem is the worst offender: **~30 files per lane (~60 total)**
of which the genuinely lane-specific content is near zero. Enumerated:

| Quarantine file (per lane)                                             | Differs between lanes by…                             |
|------------------------------------------------------------------------|-------------------------------------------------------|
| `*QuarantineStatus` (enum)                                             | **nothing** — both are `OPEN, REPLAYED, DISMISSED`    |
| `*QuarantineId`                                                        | nothing — both wrap a `String` with a `random()`      |
| `*QuarantineAccessPolicy`                                              | nothing — same 4 validation hooks                     |
| `*QuarantineReplayRegistry` + `Default*`                               | nothing — same `Map<String, Gateway>`                 |
| `Abstract*QuarantineApplicationController` (+ iface, factory, default) | record/id type only                                   |
| `AbstractSpringRest*QuarantineController` (+ iface, default)           | record/id type + URL path                             |
| `*QuarantineWebMapper`, `*QuarantineResponse`                          | which fields the record exposes                       |
| `Jpa*QuarantineStore` (195 lines each)                                 | the persistence-model field set                       |
| `*QuarantinePersistenceModel`                                          | 16 vs 18 columns (mutation adds a 2nd envelope)       |
| `*QuarantineInfrastructureAutoConfiguration` (153 lines each)          | bean prefixes + property keys                         |
| `*QuarantineRepository` (port)                                         | already trivially `QuarantineRepositoryPort<Id, Rec>` |
| `*QuarantinePayloadCodec` / `*ValueCodec` + `Serialized*`              | how many values get serialized                        |

The only *real* variation in the whole quarantine subsystem: a creation
quarantine record carries **one** replay envelope (the payload); a mutation
quarantine record carries **two** (the domain id and the payload). We already
unified the envelope itself (`QuarantineReplayEnvelope`). So the generic record
just needs to hold a small, named collection of envelopes instead of a fixed
number of fields.

### Q2: "Why do we have top-level packages `creation` / `mutation` (alongside

`domain`) under `operation`?"

Because the lanes were grown as full vertical slices and only the *vocabulary*
(`operation/domain`) and, recently, a *sliver* of the quarantine lifecycle
(`operation/quarantine`, 6 files) were pulled up to be shared. The structure
today says "two lanes plus a little shared glue." The structure we want says
"one generic operation mechanism, plus two thin specializations."

Today:

```
operation/
├── domain/        9 files   shared vocabulary            ✅ correct
├── quarantine/    6 files   shared lifecycle sliver      ✅ correct, but tiny
├── creation/      66 files  FULL vertical slice          ❌ should be thin
└── mutation/      70 files  FULL vertical slice          ❌ should be thin
```

**Target: three top-level packages that *are* the layers.** Instead of a flat
spread of capability-named packages, the operation lane should have exactly three
top-level packages, each one a classic architectural layer:

- **`domain`** — the domain layer: vocabulary, contracts, and models. No Spring,
  no JPA. Sub-packages by concept (`model`, `policy`, `handler`, `quarantine`).
- **`invocation`** — the application layer: the act of executing an operation. The
  generic engine *and* the specific invocations (`creation`, `mutation`), plus the
  quarantine capability's service/replay machinery and its driven adapters.
- **`api`** — the interface layer: the entry points (application controllers and
  Spring REST controllers).

```
operation/
├── domain/                      # domain layer — vocabulary, contracts, models (no Spring, no JPA)
│   ├── model/                   #   payload, source, family, ids, request, result, context,
│   │                            #     QuarantineReplayEnvelope, QuarantineReplayOutcome
│   ├── policy/                  #   source-aware policy framework: access · consistency · invariant ·
│   │                            #     evaluation · profile · violation  (+ per-lane "core validity" slot)
│   ├── handler/                 #   handler contracts + RegisteredHandler + HandlerRegistry
│   │   └── plan/                #     AggregateCreationPlan / AggregateMutationPlan
│   └── quarantine/              #   QuarantineRecord · QuarantineId · QuarantineStatus · lifecycle ·
│       └── port/                #     violations · access policy · QuarantineRepository (port)
│
├── invocation/                  # application layer — executing operations + the quarantine capability
│   ├── engine/                  #   generic operation engine: (load?) → handle → plan → policy → persist|quarantine
│   ├── creation/                #   THIN: DefaultCreationService, save-coordinator reuse, policy assembly
│   ├── mutation/                #   THIN: DefaultMutationService, AggregateMutationCoordinator, policy assembly
│   └── quarantine/              #   QuarantineService · replay registry/gateway/command · recorder · codec
│       └── infrastructure/      #     JpaQuarantineStore · Jackson codec · Spring auto-config (driven adapters)
│
└── api/                         # interface layer — entry points
    ├── (generic source-aware operation controller; thin creation/mutation factories)
    └── quarantine/              #   quarantine application controller + Spring REST controller + web mapper
```

The `invocation/creation` and `invocation/mutation` packages should shrink to the
*irreducible* variation (see "What Genuinely Differs" below) — roughly a handful
of files each, the way each CRUD operation is ~12 files on top of the shared
aggregate substrate.

### Where `handler` and `quarantine` live, and why

These two are the structure's stress tests. Resolving them confirms the model.

**`handler` → `domain`.** Every handler concept is domain vocabulary with no
infrastructure: the `AggregateCreationHandler` / `AggregateMutationHandler`
interfaces are the *contract the consumer implements* to express "what does this
payload mean for this aggregate"; the `AggregateCreationPlan` /
`AggregateMutationPlan` are *domain models* (the handler's output); the
`HandlerRegistry` / `RegisteredHandler` are a *domain-level dispatch contract*
(just `Map<payloadType, handler>`, no Spring/JPA). It lives in `domain/handler`,
the same way `AggregateCrudDefinition` is domain even though the engine consumes
it.

**`quarantine` → sliced across all three layers.** Quarantine is a *capability*,
not a *concept* — it is not a peer of "creation." It is a vertical slice with its
own domain/application/interface facets, exactly like creation and mutation. So it
does not pick one package; it spreads across the three layers like everything
else:

- `domain/quarantine` — the record, id, status, lifecycle contract, violations,
  access policy, and the `QuarantineRepository` **port** (pure domain).
- `invocation/quarantine` — the service, replay registry/gateway/command,
  recorder, and codec. *Replay is literally re-invocation* and recording is a
  held-back invocation, so this machinery is squarely application-layer. Its
  driven adapters (JPA store, Jackson codec, Spring auto-config) sit under
  `invocation/quarantine/infrastructure`.
- `api/quarantine` — the quarantine application controller + Spring REST
  controller + web mapper (admin surface).

That quarantine *cannot* live in just one of the three is the proof the model is
sound: a layer holds *kinds of things*; a capability *cuts through* the layers.

**One tension to record.** The template already has a top-level `infrastructure`
package (sibling of `useCases`). Strict layering would push quarantine's
JPA/Jackson/Spring adapters there, keeping `operation/{domain,invocation,api}`
purely use-case. The recommendation here is instead to **co-locate them under
`invocation/quarantine/infrastructure`**, because the quarantine subsystem is a
self-contained, auto-configured capability and co-location keeps it discoverable —
a deliberate call worth noting. (The operation lane adds *no other* infrastructure:
operation persistence flows through the CRUD lane's aggregate ports, so quarantine
is the only infra package here.)

---

## What Genuinely Differs Between Creation And Mutation

Before proposing what to merge, we must be honest about what is *real* variation.
Forcing these together would be over-abstraction. There are only four genuine
differences, and they are all small:

1. **Input state-relativity.** Creation starts from nothing. Mutation starts from
   an existing aggregate id and loads the current model before handling. This is
   the same distinction as "save vs update" in CRUD — and CRUD models it with a
   coordinator per operation, not a duplicated lane.

2. **Handler arity.** `AggregateCreationHandler` is `Payload → Plan`.
   `AggregateMutationHandler` is `(currentModel, Payload) → Plan`. Mutation needs
   the current model; creation does not.

3. **One policy slot.** Creation's core check is `CreationPolicy` ("is this a
   valid thing to create?"). Mutation's is `MutationTransitionPolicy` ("is this a
   valid before→after transition?"). The other three slots (access, invariant,
   external-consistency) are the same idea on both sides. So **3 of 4 sub-policies
   are conceptually identical**; only the "core validity" slot has a different
   name and a different evaluation input (`afterModel` vs `beforeModel+afterModel`).

4. **Quarantine envelope count.** Creation stores 1 replay envelope, mutation
   stores 2. Already reconciled by `QuarantineReplayEnvelope`; just needs a
   named collection.

**Everything else is mechanically identical or trivially parameterizable:**
request metadata, result/context shape, policy decision/bundle/profile/violation
machinery, the entire quarantine lifecycle, handler registration, the
source-aware controller sugar, the codecs, the JPA stores, the auto-configs.

That is the whole point: the surface area of *genuine* difference is ~4 small
concepts, but the surface area of *duplicated* code is ~136 files.

---

## Refactoring Proposals (Sequenced)

Ordered by leverage-to-risk. Each is independently shippable and independently
valuable. Earlier items unblock later ones. Every step must keep both the
framework tests and the sample `mvn verify` green.

### Proposal A — One generic quarantine subsystem  *(highest leverage, lowest risk)*

**Problem.** ~60 files of near-identical quarantine code across the two lanes.

**Proposal.** Collapse the quarantine subsystem into a single generic capability
sliced across the three layers (`domain/quarantine`, `invocation/quarantine`,
`api/quarantine`), parameterized by `<Id, Record>` (and a small record protocol).
The lanes contribute only:

- a small descriptor (table name, URL path, which envelopes the record carries)
- the JPA `@Entity` (or a discriminator on one shared entity)

Concretely:

- **`QuarantineStatus`** → one enum in `domain/quarantine/model` (the two enums
  are byte-identical). Delete both copies.
- **`QuarantineId`** → one generic `record QuarantineId(String value)` with
  `random()` in `domain/quarantine/model`. Delete `CreationQuarantineId` /
  `MutationQuarantineId`.
- **`QuarantineRecord<…>`** → one generic record in `domain/quarantine/model`
  implementing the existing `QuarantineLifecycleRecord`, holding
  `Map<String, QuarantineReplayEnvelope> replayInputs` (creation:
  `{"payload": …}`; mutation: `{"domainId": …, "payload": …}`), a generic
  `List<OperationPolicyViolation>` (see Proposal B), and the shared
  lifecycle/replay metadata. Delete both concrete records.
- **`QuarantineAccessPolicy`** + the **`QuarantineRepository` port** → one each in
  `domain/quarantine` (port under `domain/quarantine/port`).
- **`QuarantineReplayRegistry` (+ default)**, **`QuarantineService`**, the
  **recorder** and **codec** → one each in `invocation/quarantine`.
- **`QuarantineApplicationController` (+ abstract/default/factory)**,
  **`SpringRestQuarantineController` (+ abstract/default)**,
  **`QuarantineWebMapper` / `QuarantineResponse`** → one each in `api/quarantine`.
  The web controller takes its `@RequestMapping` path from the descriptor.
- **`JpaQuarantineStore`** + **`QuarantineInfrastructureAutoConfiguration`** → one
  each in `invocation/quarantine/infrastructure`; the autoconfig takes bean
  names/properties from the descriptor.
- **`QuarantinePersistenceModel`** → one generic `@MappedSuperclass` with the
  common 15 columns + a JSON column for `replayInputs`; two ~10-line `@Entity`
  subclasses pick the table name. (Or a single table with a `lane` discriminator.)

**Before / After.** ~60 files → ~22 generic files + 2 tiny per-lane entities.

**Risk.** Low. The quarantine API surface is admin-internal
(`/internal/*-quarantines`), not part of the generator's primary consumer
contract. No business semantics change. The persistence column set is unchanged
(envelopes already serialize the same way), so **no data migration**.

**Effort.** Medium (touches many files, but each change is mechanical).

---

### Proposal B — One generic source-aware policy framework

**Problem.** Creation and mutation each carry a full policy package tree
(`access`, `consistency`, `invariant`, `evaluation`, `profile`, `quarantine`,
`violation`) plus a ~160-line evaluating policy whose classify-and-collect logic
is near-identical (`AggregateCreationPolicies` vs `AggregateMutationPolicies`).

**Proposal.** Lift the framework into `operation/domain/policy/` (the policy
*framework* is domain; the per-aggregate *assembly* — e.g. `AggregateCreationPolicies`
wiring a definition's policies into a bundle — stays in `invocation/creation`
since it reaches into the aggregate definition):

- **`ViolationHandling`** (ALLOW/REJECT/QUARANTINE) — identical enum, share it.
- **`OperationPolicyViolation`** — generic `(kind, message, Optional<severity>)`;
  the kind becomes a generic `ViolationKind` with a stable set
  `{ACCESS, CORE, INVARIANT, EXTERNAL_CONSISTENCY}` where `CORE` is "creation
  validity" on one side and "transition validity" on the other.
- **`PolicyDecision`**, **`PolicyProfile`**, **`PolicyProfileResolver`**,
  **`PolicyBundle<Input>`** — generic. (`PolicyDecision` is already
  byte-parallel; `PolicyProfile` is 5 handlings on both sides.)
- **`SourceAwarePolicy<Input>`** + a single `EvaluatingSourceAwarePolicy<Input>`
  that runs the collect/classify loop once. `Input` is `(source, afterModel)` for
  creation and `(source, beforeModel, afterModel)` for mutation — model it as a
  small `PolicyEvaluationInput` so the loop is shared.

The lanes keep only their **sub-policy interfaces** that have genuinely different
arity (creation: `CreationPolicy(source, after)`; mutation:
`MutationTransitionPolicy(source, before, after)`), plus the lane's profile
defaults.

**Before / After.** Two ~7-package policy trees + two ~160-line evaluators → one
generic policy package + one evaluator + a thin per-lane "core validity" policy.

**Risk.** Medium. `PolicyProfile` / `ViolationHandling` are on the public surface
(consumers tune them). A generic violation kind is a visible rename. Do this as a
deliberate, documented API change *before* the generator locks on.

**Effort.** Medium-high.

---

### Proposal C — One generic request / result / context

**Problem.** `CreationRequest<Payload>` vs `MutationRequest<DomainId, Payload>`;
`CreationResult` vs `MutationResult`; `CreationContext` vs `MutationContext`.
`CreateResult(id, model)` is just `IdentifiedModel(id, model)` under another name.

**Proposal.** Generic carriers in `operation/domain/model`:

- **`OperationRequest<DomainId, Payload>`** carrying the optional `domainId`,
  payload, source, family, correlation, causation. Creation uses an absent /
  `Void` domain id; mutation supplies it. (Mirrors how the CRUD engine takes the
  same workflow regardless of operation.)
- **`OperationResult<DomainId, Model>`** = `(context, policyDecision,
  Optional<IdentifiedModel<DomainId, Model>>)`. Delete `CreateResult` (fold into
  `IdentifiedModel`). The `applied()/quarantined()/…OrThrow()` helpers are
  already identical between the two results — write them once.
- **`OperationContext<DomainId, Model>`** with optional before/after models.
  Creation leaves `before` empty; mutation fills both.

**Before / After.** Six parallel records → three generic records.

**Risk.** **High.** This is the surface the generator and consumers reference most
directly (`CreationResult`, `MutationRequest`, etc.). It must be done as a
deliberate, well-documented API consolidation, ideally with deprecated type
aliases for one release. Highest leverage for "truly generic," highest care.

**Effort.** High.

---

### Proposal D — One generic handler registry

**Problem.** `CreationHandlerRegistry` / `DefaultCreationHandlerRegistry` /
`RegisteredCreationHandler` duplicated against the mutation equivalents; the
`Map<payloadType, handler>` logic is identical.

**Proposal.** Generic `OperationHandlerRegistry<Handler>` and
`RegisteredHandler<Handler, Payload>` in `operation/domain/handler/`. The lanes
keep only their **handler functional interface** (the genuine arity difference:
`Payload → Plan` vs `(Model, Payload) → Plan`), also in `domain/handler`.

**Risk.** Low-medium. `RegisteredHandler.of(...)` is generator-visible (just
standardized in the last refactor), so keep the factory shape stable.

**Effort.** Low.

---

### Proposal E — One generic source-aware application controller

**Problem.** `CreationApplicationController` (16 methods) and
`MutationApplicationController` (16 methods) are the same source-variant sugar
(`*UserIntent`, `*InternalCommand`, `*AuthoritativeExternalEvent`,
`*ProcessEmittedAction`, each × `WithResult`) over a single core
`…WithResult(request)`.

**Proposal.** A generic
`SourceAwareOperationController<DomainId, Model, Request>` in `operation/api/`
providing all the sugar once, with thin per-lane factories alongside it.
Creation/mutation differ only in whether the request carries a domain id — which
`OperationRequest` (Proposal C) already unifies.

**Risk.** Medium (generator- and consumer-visible). Best done *after* Proposal C.

**Effort.** Medium.

---

### Proposal F — Collapse to three top-level packages (`domain` / `invocation` / `api`)

**Problem.** The current layout advertises "two lanes" (`operation/creation`,
`operation/mutation`) as peers of `operation/domain`, reinforcing duplication and
mixing layer-concepts with capability-concepts.

**Proposal.** Once A–E land, fold everything into exactly three top-level
packages, each a layer (see the Target structure under Q2):

- **`operation/domain`** — model, policy framework, handler contracts + plan, and
  the quarantine domain (record/port). Pure domain.
- **`operation/invocation`** — the generic `engine`, the thin `creation` /
  `mutation` invocations (services, coordinators, policy assembly), and the
  quarantine capability's service/replay machinery + driven adapters
  (`invocation/quarantine/infrastructure`).
- **`operation/api`** — the generic source-aware operation controller (+ thin
  per-lane factories) and the quarantine admin controllers.

`handler` resolves into `domain/handler`; `quarantine` is *sliced* across all
three layers, not given a single home (its inability to pick one is the proof the
layering is right — see "Where `handler` and `quarantine` live" under Q2).

Update `module-info.java` exports to advertise `operation/domain`,
`operation/invocation`, and `operation/api` (and their sub-packages) as the public
surface; the `creation` / `mutation` specializations live *under* `invocation` and
are no longer top-level lanes.

**Risk.** Low if done last (it is mostly the *consequence* of A–E).

**Effort.** Low-medium (mechanical moves + module-info).

---

### Proposal G — North star: creation & mutation as one parameterized operation

**Observation.** Creation and mutation are not two lanes; they are two members of
one *operation family*, exactly as save/fetch/update/delete are four members of
the CRUD family. The end-state mirrors the CRUD engine:

- a generic **operation engine** that runs: *(optionally load current) → apply
  registered handler → build plan → evaluate source-aware policy → persist or
  quarantine → post-commit*, with the "load current" step being a no-op for
  creation;
- creation and mutation as **thin coordinators/services** that plug their genuine
  variation (input shape, handler arity, core-validity policy) into that engine.

**Recommendation.** Treat G as the *direction*, not a single big-bang. Land A→F;
the engine in G emerges naturally once the substrate is shared. **Stop at "thin
specializations on a shared substrate"** — do not erase the creation/mutation
distinction entirely. The CRUD lane keeps save/fetch/update/delete as named,
distinct services for legibility; the operation lane should likewise keep
creation and mutation as named, distinct (but thin) specializations. The goal is
*no copy-paste*, not *no names*.

---

## Suggested Sequencing

```
A  Generic quarantine subsystem        ← do first: biggest win, lowest risk, admin-internal API
D  Generic handler registry            ← small, unblocks nothing but cheap and clean
B  Generic policy framework            ← medium; coordinate with consumers (profiles are tuned)
C  Generic request/result/context      ← high care; deprecated aliases for one release
E  Generic source-aware controller     ← after C (depends on unified request)
F  Collapse to domain/invocation/api   ← consequence of A–E; mechanical moves + module-info
G  (Emergent) one operation engine     ← north star; arrives as substrate consolidates
```

A and D can ship immediately and independently. B and C are the API-visible
inflection points that should land **before** the generator targets the operation
lane. E and F are clean-up that follows. G is the destination, not a discrete
task.

---

## What Should Stay Separate (Deliberate Asymmetry)

To avoid over-correcting, these should **not** be forced into one shape:

- **The handler functional interfaces.** `Payload → Plan` vs
  `(Model, Payload) → Plan` is genuine; mutation needs current state.
- **The "core validity" policy slot.** `CreationPolicy` (creatability) vs
  `MutationTransitionPolicy` (transition validity) are different questions; keep
  both, share everything around them.
- **The current-model load step.** Creation has none; mutation must load. Model it
  as a present-or-absent step in the shared engine, not as two engines.
- **The named services/coordinators.** Keep `…CreationService` /
  `…MutationService` as legible entry points, thin over the shared substrate —
  exactly as CRUD keeps four named services over one engine.

---

## Definition Of Done

The operation lane is "crisp like CRUD" when:

1. The quarantine subsystem exists **once** (one record, one service, one
   controller pair, one store, one autoconfig), parameterized — not twice.
2. Source-aware policy evaluation exists **once**; lanes contribute only their
   "core validity" policy and profile defaults.
3. Request / result / context exist **once**; `CreateResult` is gone (folded into
   `IdentifiedModel`).
4. Handler registration exists **once**; lanes contribute only the handler
   interface.
5. The source-aware controller sugar exists **once**.
6. The operation lane has exactly three top-level packages — `operation/domain`,
   `operation/invocation`, `operation/api` — and `invocation/creation` /
   `invocation/mutation` are each a *handful* of files (the genuine variation),
   not 66–70.
7. `module-info.java` advertises `domain` / `invocation` / `api` as the public
   surface; the generator targets *that*, not two parallel copies.

When that holds, adding a *third* operation family (say, a "retire"/soft-delete
operation, or a "correct"/compensating operation) costs a handful of files on the
shared substrate — the same way adding a fifth CRUD operation would — instead of
a third 60-file vertical copy.