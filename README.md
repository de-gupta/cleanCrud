# Clean CRUD Framework

`cleanCrud` is a Java framework for building CRUD modules in a consistent clean-architecture shape.

Its core promise is:

- one predictable module structure
- one predictable API -> application -> domain -> persistence flow
- reusable CRUD runtime mechanics
- separation between domain IDs and persistence IDs
- a clean default path for both standalone aggregates and aggregates that own related satellites

## Installation

Add the library to your Maven `pom.xml`:

```xml
<dependency>
    <groupId>io.github.de-gupta</groupId>
    <artifactId>cleanCrud</artifactId>
    <version>${cleanCrud.version}</version>
</dependency>
```

## Mental Model

There is still only one normal CRUD flow.

For a standalone aggregate, you declare one aggregate definition and let the framework run save, fetch, update, and
delete through the aggregate lifecycle engine.

If that aggregate owns one or more satellites, you keep the same CRUD flow and additionally declare relationship
definitions. Once those are declared, the framework orchestrates create, update, delete, fetch hydration, and response
construction for the owned satellites.

## What The Consumer Provides

For a normal standalone aggregate, the consumer provides:

- domain model, create model, update patch model, response model
- domain builder, patcher, and response builder
- insertion, patch, deletion, security, and duplicate policies
- fetch/save/update/delete persistence services
- one aggregate mutation port bean
- one aggregate fetch port bean
- one aggregate definition bean
- one shared `AggregateLifecycleEngine` bean for the application
- save/fetch/update/delete service beans created through `AggregateCrudServices`

For an aggregate with satellites, the consumer additionally provides:

- the satellite aggregate itself in the same normal shape
- one relationship definition per owned relationship
- nested create/update normalization into satellite intents
- one identity resolver
- one link strategy
- one hydration strategy

That is the only extra framework-level declaration surface. The orchestration itself remains inside `cleanCrud`.

## Minimal Standalone Aggregate

The normal standalone shape looks like this:

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

```java
@Configuration
class TaskCrudPortsConfiguration
{
    @Bean
    @Qualifier("taskAggregateMutationPort")
    AggregateMutationPort<Long, TaskDomainModel, TaskDomainModelCreate, TaskDomainModelUpdatePatch>
    taskAggregateMutationPort(
            @Qualifier("taskSavePersistenceService")
            final SavePersistenceService<Long, TaskDomainModel> savePersistenceService,
            @Qualifier("taskUpdatePersistenceService")
            final UpdatePersistenceService<Long, TaskDomainModel> updatePersistenceService,
            @Qualifier("taskDeletePersistenceService")
            final DeletePersistenceService<Long> deletePersistenceService)
    {
        return AggregateMutationPortAdapter.withPersistenceServices(
                savePersistenceService,
                updatePersistenceService,
                deletePersistenceService);
    }

    @Bean
    @Qualifier("taskAggregateFetchPort")
    AggregateFetchPort<Long, TaskDomainModel> taskAggregateFetchPort(
            @Qualifier("taskFetchPersistenceService")
            final FetchPersistenceService<Long, TaskDomainModel> fetchPersistenceService)
    {
        return AggregateFetchPortAdapter.withPersistenceService(fetchPersistenceService);
    }
}
```

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
            @Qualifier("taskAggregateMutationPort")
            final AggregateMutationPort<Long, TaskDomainModel, TaskDomainModelCreate, TaskDomainModelUpdatePatch> mutationPort,
            @Qualifier("taskAggregateFetchPort")
            final AggregateFetchPort<Long, TaskDomainModel> fetchPort,
            @Qualifier("taskDomainModelBuilder")
            final DomainModelBuilder<TaskDomainModelCreate, TaskDomainModel> createBuilder,
            @Qualifier("taskDomainModelPatcher")
            final DomainModelPatcher<TaskDomainModel, TaskDomainModelUpdatePatch> patcher,
            @Qualifier("taskDomainResponseBuilder")
            final DomainResponseBuilder<TaskDomainModel, TaskDomainModelResponse> responseBuilder,
            @Qualifier("taskInsertionPolicy")
            final InsertionPolicy<TaskDomainModel> insertionPolicy,
            @Qualifier("taskPatchPolicy")
            final PatchPolicy<TaskDomainModel> patchPolicy,
            @Qualifier("taskDeletionPolicy")
            final DeletionPolicy<TaskDomainModel> deletionPolicy,
            @Qualifier("taskDomainSecurityPolicy")
            final DomainSecurityPolicy<TaskDomainModel> securityPolicy,
            @Qualifier("taskDuplicateDefinition")
            final DuplicateDefinition<TaskDomainModel> duplicateDefinition)
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

```java
@Configuration
class TaskCrudServicesConfiguration
{
    @Bean
    SaveService<TaskDomainModelCreate, TaskDomainModelResponse, Long> taskSaveService(
            @Qualifier("taskAggregateCrudDefinition")
            final AggregateCrudDefinition<
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
            @Qualifier("taskAggregateCrudDefinition")
            final AggregateCrudDefinition<
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
            @Qualifier("taskAggregateCrudDefinition")
            final AggregateCrudDefinition<
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
    @Qualifier("taskDeleteService")
    DeleteService<Long> taskDeleteService(
            @Qualifier("taskAggregateCrudDefinition")
            final AggregateCrudDefinition<
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

This is the default `cleanCrud` shape.

## Adding One Satellite

When an aggregate owns one or more satellites, the consumer still models:

- the master aggregate normally
- the satellite aggregate normally

The additional step is to declare the relationship.

Typical relationship declaration:

```java
var versionRelationshipDefinition =
        AggregateRelationshipDefinitions
                .<Long, TaskDomainModel, TaskDomainModelCreate, TaskDomainModelUpdatePatch,
                        Long, VersionDomainModel, VersionDomainModelCreate, VersionDomainModelUpdatePatch>
                        aggregateRelationshipDefinition()
                .name("version")
                .cardinality(Cardinality.ONE)
                .lifecycleSemantics(
                        LifecycleSemanticsBuilder.lifecycleSemantics()
                                                 .cascadeCreate()
                                                 .cascadeUpdate()
                                                 .cascadeDelete()
                                                 .orphanDelete()
                                                 .hydrateOnFetch()
                                                 .build())
                .satelliteDefinition(versionAggregateCrudDefinition)
                .createInputResolver(TaskDomainModelCreate::satelliteCreateIntents)
                .patchInputResolver(TaskDomainModelUpdatePatch::satelliteMutationIntents)
                .identityResolver(taskVersionIdentityResolver)
                .reconciliationStrategy(ReconciliationStrategy.REPLACE)
                .linkStrategy(taskVersionLinkStrategy)
                .hydrationStrategy(taskVersionHydrationStrategy)
                .build();
```

Then attach it to the aggregate definition:

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
        .build();
```

After that, the framework owns:

- satellite create orchestration
- satellite update / replace / remove orchestration
- satellite delete orchestration
- fetch hydration
- response-level enrichment through the normal response builder flow

There is still no separate “orchestrated CRUD API”. It remains the same CRUD runtime with one richer aggregate
definition.

## Public DSL

The main consumer-facing aggregate DSL types are:

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
- collection reconciliation for `REPLACE` and `MERGE_BY_ID`

### Supported vs Deferred

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

Historized persistence also supports a nullable `AuditActor` on every history row. Consumers provide any
implementation through an `AuditActorSupplier`, and the framework normalizes that into its built-in persisted audit
core. If actor capture is not desired, pass `AuditActorSupplier.none()`.

Typical historized shape:

1. Define one live JPA entity
2. Define one history JPA entity
3. Define one live Spring Data repository
4. Define one history Spring Data repository by extending `TriTemporalHistoryJpaRepository`
5. Extend `AbstractHistorizedPersistenceModelJpaRepository`
6. Implement a small history snapshot factory, typically by extending
   `AbstractPersistenceHistorySnapshotFactory`

Example:

```java
@Repository
interface TaskHistoryJpaRepository extends TriTemporalHistoryJpaRepository<UUID, TaskPersistenceModelHistory>
{
}

@Component
final class TaskHistorizedJpaRepository extends AbstractHistorizedPersistenceModelJpaRepository<
        TaskPersistenceModel, UUID, TaskPersistenceModelImpl, TaskPersistenceModelHistory>
{
    TaskHistorizedJpaRepository(
            final TaskJpaRepository liveRepository,
            final TaskHistoryJpaRepository historyRepository,
            final TaskPersistenceHistorySnapshotFactory snapshotFactory,
            final AuditActorSupplier auditActorSupplier)
    {
        super(liveRepository, historyRepository, snapshotFactory, auditActorSupplier);
    }
}
```

## Architecture

`cleanCrud` keeps the same three main layers:

### API Layer

- web controllers
- application controllers
- facades
- API/domain adapters

### Domain Layer

- domain models
- create/update/response models
- builders, patchers, response builders
- policies
- security
- validation

### Infrastructure Layer

- persistence models
- repositories
- persistence adapters
- domain/persistence ID management

The aggregate runtime sits above the persistence services and orchestrates CRUD consistently across these layers.

## Example Implementation

A full example is available in the companion repository `cleanCrud-sampleImplementation`.

That sample now demonstrates both:

- normal standalone aggregates
- aggregates that own and lifecycle-manage satellites
