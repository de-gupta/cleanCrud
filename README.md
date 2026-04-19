# Clean CRUD Framework

`cleanCrud` is a Java framework for building CRUD modules in one consistent clean-architecture shape.

From a consumer point of view, the promise is:

- you keep one normal CRUD surface
- you model each aggregate normally
- if one aggregate owns others, you declare those relationships once
- the framework orchestrates create, update, delete, and fetch hydration for you

Single-aggregate CRUD is still the default path. Aggregate relationships extend that same path rather than introducing a
second “special orchestration mode”.

## Installation

Add the library to your Maven `pom.xml`:

```xml

<dependency>
    <groupId>io.github.de-gupta</groupId>
    <artifactId>cleanCrud</artifactId>
    <version>${cleanCrud.version}</version>
</dependency>
```

## Start Here

If you are new to `cleanCrud`, the most practical mental model is:

1. define each aggregate normally as if it were standalone
2. wire one `AggregateCrudDefinition` for each aggregate
3. if one aggregate owns others, add `AggregateRelationshipDefinition`s to the owning aggregate

The companion sample repository demonstrates this with:

- `Version` as a normal standalone aggregate
- `Note` as a normal standalone aggregate
- `Task` as a normal standalone aggregate
- `Task -> Version` as a `1:1` owned relationship
- `Task -> Note` as a `1:N` owned relationship

The owning aggregate usually comes last, because its relationship definitions point at the owned aggregates’
definitions.

## What A Real Consumer Module Contains

There are two different things to keep separate:

1. the **full module implementation surface**
2. the **aggregate runtime wiring surface**

The runtime wiring is only a small part of a real `cleanCrud` module.

### Full module implementation surface

A real standalone aggregate module typically contains:

- base model
- domain model
- API model
- persistence model
- create, update patch, and response DTOs
- API/domain adapters
- domain/persistence adapters
- builders
- patchers
- response builders
- duplicate definition
- insertion, patch, deletion, and security policies
- repositories
- persistence services
- facades
- controllers
- module configuration
- aggregate ports
- aggregate definition
- CRUD service beans

So if you look only at the aggregate runtime wiring examples later in this README, remember that they show just the
aggregate-specific layer, not the whole module.

This is also where `cleanCrud-generator` is useful: it removes most of the repetitive standalone module boilerplate so
you can focus on your model and the few places where you want custom behavior.

## What You Provide For A Standalone Aggregate

For one normal standalone aggregate, you provide:

- domain model
- create model
- update patch model
- response model
- builder
- patcher
- response builder
- insertion policy
- patch policy
- deletion policy
- security policy
- duplicate definition
- fetch/save/update/delete persistence services
- one aggregate mutation port bean
- one aggregate fetch port bean
- one aggregate definition bean
- CRUD service beans built from that aggregate definition

At application level, you also provide one shared:

- `PersistenceTransactionRunner`
- `AggregateLifecycleEngine`

That is enough for normal single-aggregate CRUD.

## What The Generator Gives You

`cleanCrud-generator` is best thought of in two layers:

1. it gets you to a valid standalone aggregate quickly
2. it can then help you add relationship wiring on top of that

In other words, the generator usually handles most of the repetitive standalone boilerplate, but the consumer still
decides the important relationship semantics explicitly:

- cardinality
- reconciliation strategy
- cascade create/update/delete
- orphan delete
- fetch hydration

The generator should not guess those semantics from model types alone.

## A Concrete Consumer Journey: `Version`, `Note`, Then `Task`

Suppose you want:

- `Version` as a normal standalone aggregate
- `Note` as a normal standalone aggregate
- `Task` as a normal standalone aggregate
- `Task -> Version` as `1:1`
- `Task -> Note` as `1:N`

The usual order is:

1. define and wire `Version`
2. define and wire `Note`
3. define and wire `Task`
4. add relationship definitions from `Task` to `Version` and `Note`
5. attach those relationship definitions to the `Task` aggregate definition

That gives you one normal CRUD surface for all three aggregates, with `Task` additionally lifecycle-managing its owned
aggregates.

## Wiring A Standalone Aggregate

The normal standalone runtime wiring shape is:

1. shared persistence/runtime configuration
2. aggregate ports configuration
3. aggregate definition configuration
4. CRUD services configuration

### Shared runtime

```java

@Configuration
class CommonPersistenceConfiguration
{
	@Bean
	PersistenceTransactionRunner persistenceTransactionRunner(
			final PlatformTransactionManager transactionManager)
	{
		return SpringPersistenceTransactionRunner.withTransactionManager(transactionManager);
	}

	@Bean
	AggregateLifecycleEngine aggregateLifecycleEngine(
			final PersistenceTransactionRunner persistenceTransactionRunner)
	{
		return DefaultAggregateLifecycleEngine.withTransactionRunner(persistenceTransactionRunner);
	}
}
```

### Aggregate ports

```java

@Configuration
class TaskCrudPortsConfiguration
{
	@Bean
	@Qualifier("taskAggregateMutationPort")
	AggregateMutationPort<Long, TaskDomainModel, TaskDomainModelCreate, TaskDomainModelUpdatePatch>
	taskAggregateMutationPort(
			@Qualifier("taskSavePersistenceService") final SavePersistenceService<Long, TaskDomainModel> savePersistenceService,
			@Qualifier("taskUpdatePersistenceService") final UpdatePersistenceService<Long, TaskDomainModel> updatePersistenceService,
			@Qualifier("taskDeletePersistenceService") final DeletePersistenceService<Long> deletePersistenceService)
	{
		return AggregateMutationPortAdapter.withPersistenceServices(
				savePersistenceService,
				updatePersistenceService,
				deletePersistenceService);
	}

	@Bean
	@Qualifier("taskAggregateFetchPort")
	AggregateFetchPort<Long, TaskDomainModel> taskAggregateFetchPort(
			@Qualifier("taskFetchPersistenceService") final FetchPersistenceService<Long, TaskDomainModel> fetchPersistenceService)
	{
		return AggregateFetchPortAdapter.withPersistenceService(fetchPersistenceService);
	}
}
```

### Aggregate definition

```java

@Configuration
class TaskCrudDefinitionConfiguration
{
	@Bean
	@Qualifier("taskAggregateCrudDefinition")
	AggregateCrudDefinition<
			Long,
			TaskDomainModel,
			TaskDomainModelCreate,
			TaskDomainModelUpdatePatch,
			TaskDomainModelResponse> taskAggregateCrudDefinition(
			@Qualifier("taskAggregateMutationPort") final AggregateMutationPort<Long, TaskDomainModel, TaskDomainModelCreate, TaskDomainModelUpdatePatch> mutationPort,
			@Qualifier("taskAggregateFetchPort") final AggregateFetchPort<Long, TaskDomainModel> fetchPort,
			@Qualifier("taskDomainModelBuilder") final DomainModelBuilder<TaskDomainModelCreate, TaskDomainModel> createBuilder,
			@Qualifier("taskDomainModelPatcher") final DomainModelPatcher<TaskDomainModel, TaskDomainModelUpdatePatch> patcher,
			@Qualifier("taskDomainResponseBuilder") final DomainResponseBuilder<TaskDomainModel, TaskDomainModelResponse> responseBuilder,
			@Qualifier("taskInsertionPolicy") final InsertionPolicy<TaskDomainModel> insertionPolicy,
			@Qualifier("taskPatchPolicy") final PatchPolicy<TaskDomainModel> patchPolicy,
			@Qualifier("taskDeletionPolicy") final DeletionPolicy<TaskDomainModel> deletionPolicy,
			@Qualifier("taskDomainSecurityPolicy") final DomainSecurityPolicy<TaskDomainModel> securityPolicy,
			@Qualifier("taskDuplicateDefinition") final DuplicateDefinition<TaskDomainModel> duplicateDefinition)
	{
		return AggregateCrudDefinitions
				.<Long, TaskDomainModel, TaskDomainModelCreate, TaskDomainModelUpdatePatch, TaskDomainModelResponse>
						aggregateCrudDefinition()
				.mutationPort(mutationPort)
				.fetchPort(fetchPort)
				.createBuilder(createBuilder)
				.patcher(patcher)
				.responseBuilder(responseBuilder)
				.insertionPolicy(insertionPolicy)
				.patchPolicy(patchPolicy)
				.deletionPolicy(deletionPolicy)
				.securityPolicy(securityPolicy)
				.duplicateDefinition(duplicateDefinition)
				.build();
	}
}
```

### CRUD services

```java

@Configuration
class TaskCrudServicesConfiguration
{
	@Bean
	SaveService<TaskDomainModelCreate, TaskDomainModelResponse, Long> taskSaveService(
			@Qualifier("taskAggregateCrudDefinition") final AggregateCrudDefinition<
					Long,
					TaskDomainModel,
					TaskDomainModelCreate,
					TaskDomainModelUpdatePatch,
					TaskDomainModelResponse> definition,
			final AggregateLifecycleEngine aggregateLifecycleEngine)
	{
		return AggregateCrudServices.saveService(definition, aggregateLifecycleEngine);
	}

	@Bean
	FetchService<TaskDomainModel, Long> taskFetchService(
			@Qualifier("taskAggregateCrudDefinition") final AggregateCrudDefinition<
					Long,
					TaskDomainModel,
					TaskDomainModelCreate,
					TaskDomainModelUpdatePatch,
					TaskDomainModelResponse> definition,
			final AggregateLifecycleEngine aggregateLifecycleEngine)
	{
		return AggregateCrudServices.fetchService(definition, aggregateLifecycleEngine);
	}

	@Bean
	UpdateService<TaskDomainModelCreate, TaskDomainModelUpdatePatch, TaskDomainModelResponse, Long> taskUpdateService(
			@Qualifier("taskAggregateCrudDefinition") final AggregateCrudDefinition<
					Long,
					TaskDomainModel,
					TaskDomainModelCreate,
					TaskDomainModelUpdatePatch,
					TaskDomainModelResponse> definition,
			final AggregateLifecycleEngine aggregateLifecycleEngine)
	{
		return AggregateCrudServices.updateService(definition, aggregateLifecycleEngine);
	}

	@Bean
	DeleteService<Long> taskDeleteService(
			@Qualifier("taskAggregateCrudDefinition") final AggregateCrudDefinition<
					Long,
					TaskDomainModel,
					TaskDomainModelCreate,
					TaskDomainModelUpdatePatch,
					TaskDomainModelResponse> definition,
			final AggregateLifecycleEngine aggregateLifecycleEngine)
	{
		return AggregateCrudServices.deleteService(definition, aggregateLifecycleEngine);
	}
}
```

That is the default standalone `cleanCrud` runtime shape.

## Extending `Task` With `Version` And `Note`

Now suppose:

- `Task -> Version` is `1:1`
- `Task -> Note` is `1:N`
- `Task` lifecycle-manages both

The current recommended path is the standard satellite DSL. It keeps the relationship definition small and centered on
the few things that are actually domain-specific:

- where nested create values come from
- where nested patch values come from
- how to rebuild the master with hydrated public contracts
- optional lifecycle or reconciliation overrides

The framework supplies the repetitive parts:

- lifecycle defaults
- create and patch intent resolution
- identity resolution
- link strategy
- hydration strategy
- satellite fetch plus response mapping pipeline

### `Task -> Version` (`1:1` owned satellite)

```java
var versionRelationshipDefinition =
		AggregateRelationshipDefinitions
				.<Long, TaskDomainModel, TaskDomainModelCreate, TaskDomainModelUpdatePatch,
						Long, VersionDomainModel, VersionDomainModelCreate, VersionDomainModelUpdatePatch,
						VersionAPIModelResponse, VersionAPIModelCreate, VersionAPIModelUpdatePatch,
						VersionAPIModelResponse>
						oneToOneSatellite("version", versionAggregateCrudDefinition)
				.createExtractor(TaskDomainModelCreate::version)
				.createMapper(version -> version)
				.patchExtractor(TaskDomainModelUpdatePatch::version)
				.patchMapper(VersionAPIToDomainUpdateAdapter::toDomainModelUpdatePatch)
				.patchCreateMapper(VersionAPIToDomainCreateAdapter::toDomainModelCreate)
				.removeIdExtractor(TaskDomainModelUpdatePatch::removeVersionIds)
				.currentSatellite(task -> task.versions().stream().findFirst())
				.replaceSatellite((task, version) -> task.withVersions(version.stream().toList()))
				.publicResponseMapper(identifiedResponse -> identifiedResponse.model())
				.build();
```

Default standard semantics here are:

- `Cardinality.ONE`
- `ReconciliationStrategy.REPLACE`
- `cascadeCreate = true`
- `cascadeUpdate = true`
- `cascadeDelete = false`
- `orphanDelete = false`
- `hydrateOnFetch = true`

If `Task` should fully own `Version`, you can override lifecycle semantics and turn delete/orphan delete on.

### `Task -> Note` (`1:N` owned satellite)

```java
var noteRelationshipDefinition =
		AggregateRelationshipDefinitions
				.<Long, TaskDomainModel, TaskDomainModelCreate, TaskDomainModelUpdatePatch,
						Long, NoteDomainModel, NoteDomainModelCreate, NoteDomainModelUpdatePatch,
						NoteAPIModelResponse, NoteAPIModelCreate, NoteAPIModelUpdatePatch,
						NoteAPIModelResponse>
						oneToManySatellite("note", noteAggregateCrudDefinition)
				.createExtractor(TaskDomainModelCreate::notes)
				.createMapper(note -> note)
				.patchExtractor(TaskDomainModelUpdatePatch::notes)
				.patchMapper(NoteAPIToDomainUpdateAdapter::toDomainModelUpdatePatch)
				.patchCreateMapper(NoteAPIToDomainCreateAdapter::toDomainModelCreate)
				.removeIdExtractor(TaskDomainModelUpdatePatch::removeNoteIds)
				.currentSatellites(TaskDomainModel::notes)
				.replaceSatellites(TaskDomainModel::withNotes)
				.publicResponseMapper(identifiedResponse -> identifiedResponse.model())
				.build();
```

Default standard semantics here are:

- `Cardinality.MANY`
- `ReconciliationStrategy.MERGE_BY_ID`
- `cascadeCreate = true`
- `cascadeUpdate = true`
- `cascadeDelete = false`
- `orphanDelete = false`
- `hydrateOnFetch = true`

### Reference-only relationships are also first-class

Not every relationship is lifecycle-owned. Sometimes the master should only point at already existing satellites.

For that case, use:

- `oneToOneReferencedSatellite(...)`
- `oneToManyReferencedSatellite(...)`

Those standard builders are meant for the “reference existing aggregate only” use case:

- `cascadeCreate = false`
- `cascadeUpdate = true`
- `cascadeDelete = false`
- `orphanDelete = false`
- `hydrateOnFetch = true`

Example: `Task` only refers to existing `Note`s by id.

```java
var noteReferenceRelationshipDefinition =
		AggregateRelationshipDefinitions
				.<Long, TaskDomainModel, TaskDomainModelCreate, TaskDomainModelUpdatePatch,
						Long, NoteDomainModel, NoteDomainModelCreate, NoteDomainModelUpdatePatch,
						NoteAPIModelResponse, NoteAPIModelResponse>
						oneToManyReferencedSatellite("note", noteAggregateCrudDefinition)
				.createReferenceIdsExtractor(TaskDomainModelCreate::noteIds)
				.patchReferenceIdsExtractor(TaskDomainModelUpdatePatch::noteIds)
				.removeIdExtractor(TaskDomainModelUpdatePatch::removeNoteIds)
				.currentSatellites(TaskDomainModel::notes)
				.replaceSatellites(TaskDomainModel::withNotes)
				.publicResponseMapper(identifiedResponse -> identifiedResponse.model())
				.build();
```

Effect of the reference-only DSL:

- create does **not** create new satellites
- patch does **not** create new satellites
- referenced ids are validated through the satellite fetch port
- missing ids fail with `ResourceNotFoundException`
- the master still gets hydrated public satellite contracts on fetch

### Attach the relationships to `Task`

```java
return AggregateCrudDefinitions
		.<Long, TaskDomainModel, TaskDomainModelCreate, TaskDomainModelUpdatePatch, TaskDomainModelResponse>
				aggregateCrudDefinition()
		.mutationPort(mutationPort)
		.fetchPort(fetchPort)
		.createBuilder(createBuilder)
		.patcher(patcher)
		.responseBuilder(responseBuilder)
		.insertionPolicy(insertionPolicy)
		.patchPolicy(patchPolicy)
		.deletionPolicy(deletionPolicy)
		.securityPolicy(securityPolicy)
		.duplicateDefinition(duplicateDefinition)
		.relationshipDefinition(versionRelationshipDefinition)
		.relationshipDefinition(noteRelationshipDefinition)
		.build();
```

After that, `Task` still uses the same save, fetch, update, and delete services. There is no second consumer-facing API.

## What The Framework Takes Care Of

Once relationships are declared, `cleanCrud` takes care of:

- creating owned satellites during master create when lifecycle semantics allow it
- linking referenced satellites
- validating referenced ids through the satellite fetch port
- updating owned satellites during master update when lifecycle semantics allow it
- replacing or removing owned satellites according to reconciliation strategy
- orphan deletion when configured
- cascade deletion when the master is deleted
- fetch hydration when enabled
- preserving relationship identity through the link strategy
- keeping orchestration inside the framework runtime rather than leaking it into controllers or persistence adapters

## Consumer DSL

The main consumer-facing DSL types are:

- `AggregateCrudDefinitions`
- `AggregateRelationshipDefinitions`
- `LifecycleSemanticsBuilder`

The raw interfaces are also available, but the DSL is the recommended path for normal usage.

## Supported Relationship Semantics

Single-aggregate CRUD remains the default usage model.

Current relationship runtime support includes:

- zero-relationship CRUD
- one-to-one satellite save, fetch hydration, delete, update, and put flows
- one-to-many satellite save, fetch hydration, delete, update, and put flows
- one-to-one referenced-satellite save, fetch hydration, update, and put flows
- one-to-many referenced-satellite save, fetch hydration, update, and put flows
- collection reconciliation for `REPLACE` and `MERGE_BY_ID`

### Supported vs deferred

| Area                                               | Status    |
|----------------------------------------------------|-----------|
| `Cardinality.ONE + REPLACE`                        | Supported |
| `Cardinality.MANY + REPLACE`                       | Supported |
| `Cardinality.MANY + MERGE_BY_ID`                   | Supported |
| Business-key reconciliation                        | Deferred  |
| Arbitrary graph cycles                             | Deferred  |
| Multi-level recursive orchestration                | Deferred  |
| Cross-datasource compensation                      | Deferred  |
| Distributed workflows / sagas                      | Deferred  |
| Bulk graph orchestration beyond one root aggregate | Deferred  |

## Historized Persistence

For historized persistence, `cleanCrud` provides a single high-level repository base so consumers do not need separate
save/delete/crud repository beans for the same aggregate.

Historized persistence also supports a nullable `AuditActor` on every history row. Consumers provide any implementation
through an `AuditActorSupplier`, and the framework normalizes that into its built-in persisted audit core. If actor
capture is not desired, pass `AuditActorSupplier.none()`.

Typical historized shape:

1. define one live JPA entity
2. define one history JPA entity
3. define one live Spring Data repository
4. define one history Spring Data repository by extending `TriTemporalHistoryJpaRepository`
5. extend `AbstractHistorizedPersistenceModelJpaRepository`
6. implement a small history snapshot factory, typically by extending `AbstractPersistenceHistorySnapshotFactory`

## Architecture

`cleanCrud` keeps the same three main layers:

### API layer

- web controllers
- application controllers
- facades
- API/domain adapters

### Domain layer

- domain models
- create/update/response models
- builders
- patchers
- response builders
- policies
- security
- validation

### Infrastructure layer

- persistence models
- repositories
- persistence adapters
- domain/persistence ID management

The aggregate runtime sits above the persistence services and orchestrates CRUD consistently across these layers.

## Example Implementation

A full example is available in the companion repository `cleanCrud-sampleImplementation`.

That sample demonstrates:

- normal standalone aggregates
- `Task -> Version` as a `1:1` owned relationship
- `Task -> Note` as a `1:N` owned relationship
