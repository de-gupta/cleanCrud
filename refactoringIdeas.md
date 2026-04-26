# Refactoring Ideas & Feature Gaps

Produced from a full-repo architectural review. Covers structural rot, anti-patterns,
and missing capabilities. The known persistence-level query translation gap (pushing
`FilterSpecification` down to the DB instead of filtering in memory) is explicitly
excluded — it is already tracked.

---

## Part 1 — Architecture & Code Quality

### 1.1 The Five-Layer Mechanical Pipeline (Highest Priority)

For every CRUD operation the call chain is:

```
SpringRestController
  → ApplicationController          ← no logic, pure pass-through
    → ServiceFacade                ← maps API ↔ domain types
      → Service (AbstractSave/Fetch/Update/Delete)  ← one call to engine
        → AggregateLifecycleEngine ← where work actually happens
```

The `ApplicationController` layer has zero logic. Every concrete class is:

```java
public WebModelResponse save(WebModelCreate model)
{
	return service.save(model);   // verbatim delegation
}
```

The `SpringRestController` and `ApplicationController` differ only by whether the
return type is wrapped in `ResponseEntity`. That does not justify a parallel class
hierarchy. Result: **~48 files** of scaffolding that wrap the engine, each file
containing 10–30 lines and no real decisions.

**Fix:** Collapse to three layers — `SpringRestController` (HTTP concerns only),
`ServiceFacade` (API ↔ domain mapping), `AggregateLifecycleEngine` (all logic).
Delete the `Application Controller` layer entirely. The `Abstract*Service` classes
(`AbstractSaveService`, `AbstractFetchService`, etc.) are also pure one-liners that
delegate directly to the engine — delete those too and wire the facade directly to
the engine via `AggregateCrudServices`.

**Files to delete (representative):**

- `useCases/crud/save/api/application/` (entire package)
- `useCases/crud/fetch/api/application/` (entire package)
- `useCases/crud/update/api/application/` (entire package)
- `useCases/crud/delete/api/application/` (entire package)
- `useCases/crud/save/application/service/AbstractSaveService.java`
- `useCases/crud/fetch/application/service/AbstractFetchService.java`
- `useCases/crud/update/application/service/AbstractUpdateService.java`
- `useCases/crud/delete/application/service/AbstractDeleteService.java`

---

### 1.2 Interface + Abstract Class for Every Layer (Speculative Generality)

Every layer carries both an interface and an abstract class even when:

- There is only one concrete implementation.
- The abstract class has no abstract methods (just protected constructor + delegation).
- The interface and the abstract class have identical method signatures.

Examples: `SaveServiceFacade` / `AbstractSaveServiceFacade`,
`SavePersistenceService` / `AbstractSavePersistenceService`,
`SavePersistenceModelRepository` / `AbstractPersistenceModelJpaSaveRepository`.

The interfaces for persistence repositories (`SavePersistenceModelRepository`,
`FetchPersistenceModelRepository`, etc.) exist solely to be implemented by one
abstract class which is itself never directly instantiated. They provide no seam
for testing or substitution that the abstract class itself doesn't already provide.

**Fix:** Where there is only one concrete implementation and no planned variation,
collapse interface + abstract class into a single concrete or abstract class.
Reserve interfaces for genuine ports (already done correctly with
`AggregateFetchPort`, `AggregateMutationPort`).

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

### 1.7 Dead Type Parameters on Service Classes

`AbstractSaveService` is declared with five type parameters:

```java
public abstract class AbstractSaveService<
		MasterDomainId,
		MasterDomainModel,           // never used in this class body
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch, // never used in this class body
		MasterDomainModelResponse>
```

`MasterDomainModel` and `MasterDomainModelUpdatePatch` are dragged along solely to
satisfy the `AggregateCrudDefinition` generic signature. Same pattern in
`AbstractFetchService`, `AbstractUpdateService`, `AbstractDeleteService`.

**Fix:** This is a downstream symptom of the unnecessary service layer. Resolves
naturally when those abstract service classes are deleted (see 1.1).

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

### 2.1 No Domain Events / Lifecycle Hooks

The engine executes saves, updates, and deletes internally with no extension points.
There is no way for consumers to react to lifecycle events (post-save, post-update,
post-delete) without modifying engine internals or wrapping ports.

**Missing:** An `AggregateLifecycleListener<M>` or `AggregateEventPublisher` hook on
`AggregateCrudDefinition` that the engine calls after successful operations. Consumers
plug in their event bus, audit log, cache invalidation, etc. without touching the engine.

---

### 2.2 No Many-to-Many Relationship Support

The standard builder library covers:

- One-to-one owned satellite
- One-to-many owned satellite
- One-to-one referenced satellite
- One-to-many referenced satellite

There is no builder for many-to-many relationships (e.g., a resource linked to
multiple tags where a tag also belongs to multiple resources). The cardinality enum
has `ONE_TO_ONE` and `ONE_TO_MANY` — `MANY_TO_MANY` is absent.

**Missing:** `StandardManyToManySatelliteRelationshipBuilder` with a join-table or
intermediate entity strategy. Reconciliation would need a join-level identity resolver.

---

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

### 2.9 `Relationship` Domain Model Is Disconnected from the Runtime Engine

`Relationship`, `Relationships`, and `RelationshipBuilder` in `domain.relationship`
use raw `Class<?>` references for type information (e.g., `satelliteDomainIdType`,
`satellitePersistenceIdType`). The actual runtime engine uses `AggregateRelationshipDefinition`
with full generics.

These appear to be two separate representations of the same concept with no bridge:
one is a schema/metadata model (erased types), the other is the live runtime contract
(typed generics). It is unclear what currently consumes the `Relationships` interface
or how the two representations stay consistent.

**Missing (or to clarify):** Either a registry that maps `Relationship` metadata
entries to their corresponding `AggregateRelationshipDefinition` at startup, or —
if `Relationship` is purely for documentation/reflection-based tools — explicit
documentation of that purpose and a clear boundary preventing the schema model from
being confused with the runtime definition.

---

### 2.10 No Built-in Validation Framework Integration

`Validatable.validate()` is a custom marker interface. `AbstractModelBuilder` calls
`model.validate()` after build. However, there is no integration with Bean Validation
(JSR-380 / `jakarta.validation`). Consumers who already annotate their domain models
with `@NotNull`, `@Size`, etc. get no automatic enforcement — they must implement
`validate()` manually and duplicate constraint declarations.

**Missing:** An optional `BeanValidationModelValidator` that implements the
`validate()` call via `jakarta.validation.Validator`, allowing annotation-driven
validation to work alongside or instead of the custom `Validatable` contract.

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