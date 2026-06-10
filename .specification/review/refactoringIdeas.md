# Refactoring Ideas & Feature Gaps

Produced from a full-repo architectural review. Covers structural rot, anti-patterns,
and missing capabilities. The known persistence-level query translation gap (pushing
`FilterSpecification` down to the DB instead of filtering in memory) is explicitly
excluded — it is already tracked.

---

## Part 1 — Architecture & Code Quality
---

### 1.3 Infrastructure Code Inside the Use Cases Package

```
useCases/crud/common/infrastructure/persistence/repository/
  AbstractPersistenceModelJpaRepositorySupport.java
  AbstractHistorizedPersistenceModelJpaRepositorySupport.java
```

These are JPA-backed repository base classes living inside a `useCases` package.
This is a direct violation of the dependency rule the architecture otherwise upholds.

**Fix:** Move both classes to `infrastructure/persistence/repository/` where they
belong alongside the rest of the JPA infrastructure.

---

### 1.4 The `castDown` / `castUp` Generics Hack

`AbstractPersistenceModelJpaRepositorySupport` exposes:

```java

@SuppressWarnings("unchecked")
private ConcretePersistenceModel castDown(PersistenceModel persistenceModel)
{
	return (ConcretePersistenceModel) persistenceMo
```

This is a forced unchecked downcast with suppressed warning. It exists because the
class accepts both a `PersistenceModel` (abstract) and a
`ConcretePersistenceModel extends PersistenceModel` and the JPA repository is only
parameterized on the concrete. The two-type-parameter trick is the type system
signalling the hierarchy is wrong: if the repository always holds concrete instances,
it should be parameterized only on the concrete type from the start.

**Fix:** Eliminate the base/concrete split. Parameterize repository support classes
directly on the persistence model type and remove the cast pair entirely.

---

### 1.5 Spring Data Types Leaking Through All Layers

`org.springframework.data.domain.Pageable` and `Slice` appear in:

- `AggregateLifecycleEngine` (interface + implementation)
- `AbstractFetchService`
- `AbstractFetchServiceFacade`
- `AbstractFetchPersistenceService`

Spring Data is infrastructure. It crosses every layer boundary unchecked. If the
architecture ever needs to support a non-Spring-Data persistence backend (or tests
without the Spring context), every layer is coupled to it.

**Fix:** Define domain-owned pagination types in the port layer
(`AggregateFetchPort`). Translate to/from `Pageable`/`Slice` only at the
infrastructure boundary. This isolates the Spring Data dependency to the
infrastructure package where it already correctly lives for everything else.

---

### 1.6 The Facade Is Not a Facade — It Is an Adapter Runner

`AbstractSaveServiceFacade` holds two functional interfaces (`APIToDomainCreateAdapter`,
`DomainToAPIResponseAdapter`) and a `SaveService`, then:

```java
public APIModelResponse save(APIModelCreate model)
{
	return responseMapper.mapToAPIModelResponse(
			service.save(createMapper.mapToDomainModelCreate(model)));
}
```

This is not a Facade (which simplifies a complex subsystem). It is a mechanical
adapter pipeline. The name misleads readers about what the class actually does.

**Fix:** Rename to `*Adapter` or `*Translator`, or inline the mapping into the
controller and remove the layer. Since the controller is the only caller and the
mapping is the controller's responsibility (translating HTTP input to domain input),
the mapping naturally belongs at the controller boundary.

---

### 1.8 `BEST_EFFORT` Bulk Operations Silently Swallow Exceptions

In `DefaultAggregateLifecycleEngine`:

```java
// tryDeleteById
catch(DomainException ignored){}

// tryUpdateById
		catch(
DomainException e){return Optional.

empty(); }
```

`BEST_EFFORT` mode discards all errors with no record of what failed or why. Callers
receive a partial result set with no way to know which items were skipped or what
the failures were.

**Fix:** Return a result type that carries both successful results and a list of
per-item failures (ID + cause). A sealed interface or a simple record with
`successes` and `failures` collections would make BEST_EFFORT actually useful.

---

### 1.9 `AggregateCrudDefinitionBuilder` Setter Verbosity

Every setter on `AggregateCrudDefinitionBuilder` repeats the full 5-parameter generic
bound in its return type — 13 setter methods × ~4 lines of type params each. The
builder is 210 lines for what is essentially a record constructor.

**Fix:** This is a known Java limitation with fluent builders on generic classes. One
practical option: collapse the builder into a factory method that accepts all required
fields positionally (since all 10 fields are required anyway), removing the step-by-step
builder entirely. The fluent API only adds value when some fields are optional.

---

### 1.10 `processExistingDomainIDsConflicts` Is a Complex Recursive Algorithm Without Tests

`AbstractDomainPersistenceIDManagement.processExistingDomainIDs` is a multi-pass,
recursive, concurrent ID collision resolution algorithm — 80 lines, inline comments,
nested maps, recursive call. This is the most complex method in the infrastructure
layer. There are no unit tests specifically targeting it.

**Fix:** Extract into a dedicated `DomainIDConflictResolver` class. Write unit tests
for the edge cases (no conflicts, single conflict, cascading conflicts, concurrent
generation). The algorithm's correctness is hard to reason about in-situ.

---

## Part 2 — Feature Gaps

### 2.3 No Soft Delete Support

`DeletionPolicy` validates whether deletion is allowed, but physical deletion is the
only outcome the engine produces. There is no framework-level concept of marking a
record as logically deleted while retaining it in persistence.

Consumers who need soft delete today must implement it entirely themselves in their
persistence adapter, making the deletion policy and engine delete path redundant for
their use case.

**Missing:** A `DeletionSemantics` on `AggregateCrudDefinition` (hard delete vs soft
delete). Soft delete would call a `mutationPort().softDelete(id)` instead of
`mutationPort().delete(id)`, and the fetch port would need to filter out logically
deleted records by default.

---

### 2.4 No Optimistic Locking / Concurrency Control

Nothing in the domain model, the ports, or the engine represents a version or ETag.
Concurrent updates to the same resource are not detected — last write wins silently.

**Missing:** An optional `version` field in `AggregateCrudDefinition` (or on the
domain model via a marker interface). The engine would pass the expected version
through to the mutation port, which enforces it at the persistence boundary. Conflict
throws a `ResourceStateConflictException` (already exists in the exception hierarchy,
currently unused in this context).

---

### 2.5 No Upsert Operation by Business Key

The engine has `save` (create), `putAtId` (replace by ID), and `updateById` (patch
by ID), but no upsert that operates on a business key rather than a persistence ID.
Consumers who need "create if not exists, update if exists" by natural key have to
implement the fetch + conditional save themselves.

**Missing:** An `upsertBy(definition, businessKeyExtractor, model)` operation on the
engine that uses `duplicateDefinition` (already on the definition) to locate any
existing record by key, then either creates or patches.

---

### 2.6 No Cursor / Keyset Pagination

The only pagination mechanism is offset-based via Spring Data's `Pageable`/`Slice`.
Offset pagination is unstable under concurrent inserts and degrades at high offsets
(full table scan up to offset). Keyset (cursor-based) pagination is absent from both
the engine interface and the port contracts.

**Missing:** A `CursorPage<T>` domain type and a corresponding `findAll(definition,
cursor)` on the engine. This would also help resolve the Spring Data leak (issue 1.5)
since a domain cursor type has no Spring Data dependency.

---

### 2.7 No Observability Hooks

The engine has no instrumentation extension points. There is no way to plug in
Micrometer metrics, OpenTelemetry spans, or structured logging without wrapping every
engine call externally.

**Missing:** An `AggregateLifecycleMetrics` or `AggregateLifecycleTracer` strategy
slot on the engine or on `AggregateCrudDefinition`. At minimum, operation start/end
with the definition name and result status. The engine already has clear entry/exit
points per operation.

---

### 2.8 Tri-Temporal History Has No Use-Case Query Abstraction

`TriTemporalHistoryRepository` and `TriTemporalHistoryModel` are infrastructure
classes. There is no port or use-case service that exposes time-travel queries
(e.g., "what was the state of entity X at time T?", "what changed between T1 and T2?")
to consumers at the domain or use-case layer.

History is recorded faithfully, but only readable by reaching directly into
infrastructure. If a consumer wants to expose a "history" endpoint, they bypass all
the clean layering the rest of the framework provides.

**Missing:** A `AggregateHistoryPort<DomainId, Snapshot>` and a corresponding engine
method or separate `AggregateHistoryEngine` that queries history through the domain
layer. The translation from `TriTemporalHistoryModel` to a domain snapshot type would
live in the infrastructure adapter.

---

### 2.9 `domain.relationship` Package Serves an Undocumented Dual Role

**Status: the declaration/runtime gap is now closed.** The addition of
`StandardRelationshipLoweringSupport`, `RelationshipDrivenAggregateRelationshipBuilder`,
and the `AggregateRelationshipDefinitions.fromRelationship()` entry point provides a
full bridge from the declaration model to the typed runtime definition. A consumer
supplies a `Relationship` object and a satellite `AggregateCrudDefinition`, provides
two lambdas (`current`/`replace` or `currentMany`/`replaceMany`), and the lowering
support derives everything else — cardinality, lifecycle semantics, reconciliation
strategy, create/patch input resolvers, identity resolver, link strategy, and hydration
strategy — from the `Relationship` declaration. `LifecycleSemantics` and
`ReconciliationStrategy` from `domain.relationship` are shared directly by both the
declaration model and the runtime contract (`AggregateRelationshipDefinitionContract`),
so no translation is needed across the boundary.

**What remains:** The `domain.relationship` package (`Relationship`, `Relationships`,
`RelationshipBuilder`, `LifecycleSemantics`, `ReconciliationStrategy`, `RelationshipKind`)
now plays two distinct roles simultaneously:

1. **Generator input vocabulary** — the code generator reads `Relationships`
   implementations to produce all CRUD boilerplate. These types are the generator's
   primary API contract with the consumer project.

2. **Runtime declaration vocabulary** — the same types feed the `fromRelationship()`
   builder at runtime and their values flow unchanged into `AggregateRelationshipDefinitionContract`.

Neither role is documented anywhere in the framework's public API. Framework consumers
who do not use the generator see `Relationships`, `RelationshipBuilder`, and
`LifecycleSemantics` exported in `module-info.java` with no explanation of when or why
to implement them. Consumers who do use the generator must understand that their
`Relationships` implementation is both a code-generation input (read at build time by
the generator's `JavaGenerationSpecificationLoader`) and a runtime declaration (read at
application startup by `fromRelationship()`).

This dual role also means that `LifecycleSemantics` and `ReconciliationStrategy` are
simultaneously part of the framework's semver public API surface and part of the
generator's input contract. Any breaking change to these types breaks both existing
runtime users and the generator.

**Fix:** Document the `domain.relationship` package's dual role explicitly — either via
package-level Javadoc or a dedicated section of the README. Clarify the lifecycle:
"implement `Relationships` once; the generator reads it at build time, `fromRelationship()`
reads it at runtime." This is the missing contract documentation, not a code change.

---

### 2.11 No Satellite-Level Fetch Batching (N+1 Risk)

Looking at `AggregateFetchCoordinator`, when fetching a collection of masters each
with satellites, the hydration strategy is invoked per master. If the standard
hydration strategy fetches the satellite port per master, this produces N+1 port calls
for a collection of N masters.

**Missing (or verify):** A batch hydration path in `SatelliteHydrationStrategy` that
receives all masters at once and resolves satellites in a single port call. The current
strategy interface signature is per-master only:

```java
MasterDomainModel hydrate(
		IdentifiedModel<MasterDomainId, MasterDomainModel> master,   // one master
		AggregateFetchPort<SatelliteDomainId, SatelliteDomainModel> satelliteFetchPort,
		SatelliteLinkStrategy<...>satelliteLinkStrategy);
```

A companion `hydrateAll(Collection<IdentifiedModel<...>>, port, linkStrategy)`
defaulting to per-item loop would let implementations opt into batching.