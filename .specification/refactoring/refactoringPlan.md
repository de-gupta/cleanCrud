# Proposal A — Generic Quarantine Subsystem (domain-first), CRUD-shaped

## Context

The operation lane is two near-complete parallel copies (creation + mutation); the
quarantine subsystem is the worst offender (~60 files differing only by a type
parameter and a name prefix). This proposal collapses it into one generic,
**domain-honest, CRUD-shaped** capability, and establishes the shared homes the
rest of the lane will move into in later proposals.

**Guiding principles (user):**

- Start from honest domain truth; persistence/everything else is ceremony that
  honours it; build adapters only where needed (exactly how the CRUD lane works).
- The operation lane is a **mini-CRUD**: mirror the CRUD lane's package structure
  (`api/{application,web}`, `facade`, `application/service`, `infrastructure`) over
  a shared substrate — **only where required** — just without REST on the core
  operations (only quarantine has admin REST).
- No backward-compat concern: unreleased, only the sample (our test bed) consumes
  it. Break/rename with impunity.

Pulls forward the *minimal* slice of Proposal B: unify the policy **violation**
value object (removes a `<Violation>` type parameter from the record). The rest of
the policy-framework genericization stays in B.

## Verdict on structure: mirror the CRUD lane

```
operation/
├── domain/                          # shared domain language (no Spring/JPA)
│   ├── model/                       #   payload, source, family, ids, envelope, outcome
│   ├── policy/                      #   policy framework; A ADDS policy/violation/ (unified)
│   └── handler/ (+ plan/)           #   handler contracts, registry, plans
│
├── aggregate/                       # SHARED substrate (the crud/aggregate analogue):
│                                    #   generic operation engine, policy evaluator, service-support
│                                    #   [minimal in A; grows in B/G]
│
├── creation/                        # THIN slice (like crud/save) — layers ONLY where required
│   ├── api/application/             #   CreationApplicationController            (no api/web, no facade)
│   └── application/service/         #   CreationService, DefaultAggregateCreationService
│
├── mutation/                        # THIN slice (like crud/update)
│   ├── api/application/
│   └── application/service/         #   + AggregateMutationCoordinator
│
└── quarantine/                      # FULL CRUD-shaped capability (it DOES have api/web)
    ├── domain/
    │   ├── model/                   #   QuarantineRecord<P>, QuarantineId, QuarantineStatus,
    │   │                            #     OperationInvocationMetadata, CreationReplayInputs, MutationReplayInputs
    │   ├── QuarantineLifecycleRecord
    │   ├── policy/                  #   QuarantineAccessPolicy<P>
    │   └── port/                    #   QuarantineRepository<P>, QuarantineRepositoryPort
    ├── application/service/         #   QuarantineService<P>, DefaultQuarantineService, AbstractQuarantineService,
    │                                #     QuarantineRecorder, QuarantineReplayExecutor, ReplayOutcome,
    │                                #     QuarantineReplayRegistry/Default, QuarantineReplayGateway, QuarantineReplayCodec
    ├── api/
    │   ├── application/             #   QuarantineApplicationController<P> (+ Abstract, Default, Controllers)
    │   └── web/                     #   SpringRestQuarantineController (+ abstract) + 2 thin @RestController,
    │                                #     QuarantineResponse, QuarantineWebMapper<P>, OperationPolicyViolationResponse
    └── infrastructure/
        ├── persistence/            #   JpaQuarantineStore<P>, QuarantinePersistenceModel (@MappedSuperclass)
        │                           #     + 2 thin @Entity, JacksonQuarantineReplayCodec
        └── spring/                 #   2 thin auto-configs (per-lane entity scan/path/prefix; GENERIC beans)
```

This is the CRUD lane's exact shape: `domain` (shared language), `aggregate`
(shared substrate), thin per-operation slices, and quarantine as a self-contained
CRUD-shaped capability. A only touches creation/mutation's quarantine wiring +
violation usage; their full reshaping is Proposal F.

## The honest domain model

A quarantine record = *an operation invocation policy held back, kept for
inspection and replay.*

```
QuarantineRecord<P>                       // P = lane-typed replay inputs
    QuarantineId            id
    String                 aggregateKey
    OperationInvocationMetadata metadata  // source, family, correlation, causation — SHARED VO, not a type param
    P                      replayInputs    // CreationReplayInputs | MutationReplayInputs — typed, NOT a stringly map
    List<OperationPolicyViolation> violations
    QuarantineStatus       status
    Instant quarantinedAt, updatedAt; int replayAttemptCount
    Optional<Instant> lastReplayAt; Optional<QuarantineReplayOutcome> lastReplayOutcome; Optional<String> lastReplaySummary
implements QuarantineLifecycleRecord<QuarantineId, QuarantineRecord<P>>
```

Lane replay inputs (composed of the existing `domain/model/QuarantineReplayEnvelope`):

- `CreationReplayInputs(QuarantineReplayEnvelope payload)`
- `MutationReplayInputs(QuarantineReplayEnvelope domainId, QuarantineReplayEnvelope payload)`

Rationale: metadata is identical across families → shared VO (honesty over
speculative generality). Replay inputs are the genuine lane variation → typed
value object, never a `Map<String,…>` (that was the stringly-typed smell we
removed). Violations differ only by kind name (`CREATION` vs `TRANSITION`) → unify
`ViolationKind {ACCESS, CORE, INVARIANT, EXTERNAL_CONSISTENCY}`.

## Persistence as an adapter (honours the domain, invisible to it)

- **Port (domain):** `QuarantineRepository<P> extends QuarantineRepositoryPort<QuarantineId, QuarantineRecord<P>>`.
- **Adapter (infra):** ONE generic `JpaQuarantineStore<P>` class, instantiated per
  lane with `Class<P>` (Jackson) + the lane entity. `replayInputs` + `violations`
  serialize to JSON columns (envelopes + enums + strings — uniformly serializable);
  metadata stays flat/queryable. No per-lane persistence adapter is written — the
  generic store copes because every `P` is envelope-composed.
- **Persistence model:** one `@MappedSuperclass QuarantinePersistenceModel`
  (common columns + `replay_inputs_json` + `violations_json`) and two ~10-line
  `@Entity` subclasses choosing the table. DDL changes (typed payload columns → one
  JSON column); fine, no data to migrate.

## What each lane still contributes (thin)

- `CreationReplayInputs` / `MutationReplayInputs` (domain value object)
- the **recorder** hook (build `QuarantineRecord<P>` from the lane submission via
  `QuarantineReplayCodec`) and the **replay-executor** hook (decode `P` → typed →
  call the lane operation service → map result to `ReplayOutcome`), wired in the
  lane's thin auto-config
- the operation service already *is* the replay gateway
  (`DefaultAggregateCreationService implements …ReplayGateway`) — unchanged except
  it uses the unified codec
- a thin `@Entity`, a thin auto-config, a thin `@RestController` (path)

## Violation unification (minimal slice of B)

- New `operation/domain/policy/violation/`:
  `OperationPolicyViolation(ViolationKind, String message, Optional<InvariantSeverity>)`,
  `ViolationKind`, shared `ViolationHandling`.
- Evaluators (`AggregateCreationPolicies`, `AggregateMutationPolicies`) build
  `OperationPolicyViolation` (sub-policies still return `Optional<String>` — unchanged).
- `CreationPolicyDecision`/`MutationPolicyDecision`, the `…QuarantineRequest`
  artifacts, and web responses carry `OperationPolicyViolation`.
- Delete `CreationPolicyViolation`, `MutationPolicyViolation`,
  `CreationViolationKind`, `MutationViolationKind`, both `*PolicyViolationResponse`.
- Out of scope (stays in B): genericizing `SourceAwarePolicy`/`PolicyBundle`/
  `PolicyProfile`/the evaluator loop.

## Execution order (each step compiles; both suites green at marked steps)

1. **Homes + shared moves.** Create `operation/domain/policy/violation`,
   `operation/quarantine/{domain,application/service,api/{application,web},infrastructure/{persistence,spring}}`.
   Move the 6 existing shared quarantine files (`AbstractQuarantineService`,
   `QuarantineRecorder`, `QuarantineReplayExecutor`, `ReplayOutcome`,
   `QuarantineLifecycleRecord`, `QuarantineRepositoryPort`) into the new homes.
   Update imports + `module-info.java`. **Build green.**
2. **Violation unification.** Add `domain/policy/violation/*`; rewire evaluators,
   decisions, quarantine requests, responses; delete the 4 lane violation/kind
   types + 2 responses. **Build + test green.**
3. **Generic domain.** `QuarantineId`, `QuarantineStatus`,
   `OperationInvocationMetadata`, `QuarantineRecord<P>`, the two `*ReplayInputs`,
   `QuarantineAccessPolicy<P>`, `QuarantineRepository<P>`.
4. **Generic application/service.** `QuarantineService<P>`,
   `DefaultQuarantineService`, registry/gateway, `QuarantineReplayCodec`; delete
   `Serialized*` types.
5. **Generic infrastructure.** `JpaQuarantineStore<P>`,
   `JacksonQuarantineReplayCodec`, `QuarantinePersistenceModel` superclass + 2 thin
   entities.
6. **Generic api.** generic application + REST controllers, web mapper/response.
7. **Rewire the 2 auto-configs** to build generic beans + lane recorder/executor
   hooks; delete the per-lane record/id/status/store/service/controller/codec/
   registry/access-policy/web files. **Build green.**
8. **Update sample + tests.** `cleanCrud-sampleImplementation` configs reference
   only generic quarantine types; framework quarantine tests fold to generic types.
   **Both suites green.**

## Verification

- Quick compile check at any point: `~/.claude/scripts/run-quiet.sh ~/.claude/scripts/mvn-verify.sh`
  (this is `test-compile`).
- Framework build + tests after milestones 1, 2, 7, 8:
  `~/.claude/scripts/run-quiet.sh ~/.claude/scripts/mvn-clean-build.sh` then
  `~/.claude/scripts/run-quiet.sh ~/.claude/scripts/mvn-test.sh`.
- Sample (full, incl. quarantine ITs — Testcontainers): in
  `cleanCrud-sampleImplementation`, `~/.claude/scripts/run-quiet.sh bash -c "mvn clean verify -q"`.
  `-Pfast` runs the *faster IT subset* but still uses Testcontainers (not a skip).
  If the WSL2 stale-class `ApplicationContext` flake appears, `rm -rf target` and
  retry once.
- Expected outcome: quarantine ~60 files → ~22 generic + ~8 thin lane; one
  `OperationPolicyViolation`; replay still round-trips (JPA store tests confirm
  envelope + violation persistence); creation/mutation slices unchanged except
  quarantine wiring + violation usage.