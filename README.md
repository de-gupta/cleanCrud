# cleanCrud

`cleanCrud` is a Java framework for building CRUD modules in one consistent clean-architecture shape.

Its core promise is:

- one normal CRUD surface for each aggregate
- one normal way to structure modules
- explicit relationship declarations when one aggregate owns or references another
- framework-managed orchestration for create, update, delete, and fetch hydration

Single-aggregate CRUD is still the default path. Relationships extend that path; they do not introduce a second “special
orchestration mode”.

## Installation

Add `cleanCrud` to your Maven build:

```xml
<dependency>
    <groupId>io.github.de-gupta</groupId>
    <artifactId>cleanCrud</artifactId>
    <version>${cleanCrud.version}</version>
</dependency>
```

## What Consumers Actually Provide

There are two different scopes to keep separate:

1. the **full module implementation surface**
2. the **relationship/runtime declaration surface**

### Full module implementation surface

A real `cleanCrud` module still contains the usual pieces:

- base model
- domain model
- API model
- persistence model
- create / update patch / response DTOs
- API/domain adapters
- domain/persistence adapters
- builders
- patchers
- response builders
- duplicate definition
- insertion / patch / deletion / security policies
- repositories
- persistence services
- facades
- controllers
- module configuration
- aggregate ports
- aggregate definition
- CRUD service beans

`cleanCrud-generator` exists because most of this is repetitive.

### Minimal conceptual inputs

From a consumer point of view, the important inputs are now:

- a base model
- optional relationship declarations next to that model
- the normal aggregate-specific builders / patchers / policies / persistence services

The generator can derive most of the repetitive structure from the first two.

## Standalone Aggregate First

For a normal standalone aggregate, the runtime wiring is still:

1. shared runtime configuration
2. aggregate fetch/mutation ports
3. one `AggregateCrudDefinition`
4. CRUD service beans from that definition

At application level you still provide:

- `PersistenceTransactionRunner`
- `AggregateLifecycleEngine`

That is enough for normal single-aggregate CRUD.

## Post-Commit Hooks And Durable Processes

`cleanCrud` now supports two different kinds of post-transaction behavior.

### Lightweight post-commit hook

`AggregateCrudDefinition.postCommitMutation(...)` remains the small,
best-effort option.

Use it for things like:

- logging
- metrics
- notifications
- fire-and-forget downstream publication

Its current behavior is:

- the aggregate mutation commits first
- the hook runs after commit
- execution is asynchronous and best effort
- failures are logged and do not fail the main CRUD call

That feature stays intentionally small.

### Durable process

For richer follow-up workflows, `cleanCrud` now has a separate durable process
subsystem.

This is for cases where the application needs to:

- persist an aggregate
- start a durable follow-up process in the same transaction
- retry with policy
- survive crashes and restarts
- emit internal application commands or events
- mutate domain state again based on the external outcome

Conceptually, the runtime flow is:

1. aggregate mutation commits
2. durable process task is persisted atomically with that mutation
3. after commit, the framework immediately nudges execution of the new task
4. if execution succeeds, emitted internal application actions are dispatched
5. if execution fails transiently, retry metadata is persisted
6. a framework-owned poller later picks up due work

This is intentionally separate from `postCommitMutation(...)`.

### What the framework provides

The durable process subsystem now includes:

- durable process domain contracts
- `DurableProcessStartRequest`
- `DurableProcessRunner`
- `RetryPolicy` and `BackoffPolicy`
- immediate after-commit execution nudge
- JPA-backed durable task persistence
- framework-owned Spring polling scheduler
- in-memory durable task store for dev/test use

Consumers typically provide:

- durable process definitions/executors
- an `ApplicationActionDispatcher`
- optional polling configuration overrides

### Default infrastructure behavior

The Spring infrastructure is provided by the framework itself.

The current defaults are:

- durable process infrastructure enabled
- polling enabled
- poll interval `PT1M`
- batch size `100`

Properties live under:

- `clean-crud.process.enabled`
- `clean-crud.process.polling-enabled`
- `clean-crud.process.poll-interval`
- `clean-crud.process.batch-size`

### Current operational boundary

This feature is in a good place for application-local durable workflows, but it
should still be described accurately.

Current strengths:

- atomic process start with aggregate mutation
- immediate after-commit execution
- persisted retry metadata
- framework-owned recovery polling
- clean routing of follow-up actions through the application layer

Current limit:

- the first hardening pass is single-node friendly
- multi-node claiming/locking is not implemented yet
- this is not intended to be a DAG/workflow orchestrator

The goal is durable application processes, not a general-purpose job platform.

## Relationship Declarations Are Now A Domain Concept

Relationships are no longer a generator-only idea.

They live in `cleanCrud` as domain concepts under:

- `de.gupta.clean.crud.template.domain.relationship`

The central types are:

- `Relationship`
- `RelationshipKind`
- `Relationships`
- `LifecycleSemantics`
- `ReconciliationStrategy`

This means:

- consumers declare relationships once
- the generator reads those declarations
- runtime code can also use those same declarations

So `Relationship` is the semantic source of truth.

## Consumer Input Shape For Relationships

The recommended consumer shape is:

1. a generic base model
2. a sibling `*Relationships` class implementing `Relationships`

Example:

```java
public interface PersonModel<T, V, N>
{
    String firstName();

    T tag();

    V currentVersion();

    Optional<V> lastKnownVersion();

    Collection<N> notes();
}
```

And next to it:

```java
public final class PersonRelationships implements Relationships
{
    @Override
    public Class<?> baseModelClass()
    {
        return PersonModel.class;
    }

    @Override
    public Collection<Relationship> relationships()
    {
        return List.of(
                Relationship.referenced("tag", TagModel.class)
                            .satelliteApiIdType(Long.class)
                            .satelliteDomainIdType(Long.class)
                            .satellitePersistenceIdType(UUID.class)
                            .build(),
                Relationship.referenced("currentVersion", VersionModel.class)
                            .satelliteApiIdType(Long.class)
                            .satelliteDomainIdType(Long.class)
                            .satellitePersistenceIdType(UUID.class)
                            .build(),
                Relationship.referenced("lastKnownVersion", VersionModel.class)
                            .satelliteApiIdType(Long.class)
                            .satelliteDomainIdType(Long.class)
                            .satellitePersistenceIdType(UUID.class)
                            .build(),
                Relationship.owned("notes", NoteModel.class)
                            .satelliteApiIdType(Long.class)
                            .satelliteDomainIdType(Long.class)
                            .satellitePersistenceIdType(UUID.class)
                            .reconciliationStrategy(ReconciliationStrategy.MERGE_BY_ID)
                            .build());
    }
}
```

### What the consumer explicitly says

Each `Relationship` declares:

- property name
- target aggregate base model
- `OWNED` or `REFERENCED`
- satellite API id type
- satellite domain id type
- satellite persistence id type
- lifecycle semantics
- reconciliation strategy where relevant

### What the consumer does not have to say

Cardinality is not declared explicitly.

The generator infers it from the base-model property shape:

- `U` -> required `ONE`
- `Optional<U>` -> optional `ONE`
- `Collection<U>` -> `MANY`

So the semantic model is:

- base model shape tells the framework **how many**
- `Relationship` tells the framework **what kind**

## Relationship Semantics

### `RelationshipKind`

There are two first-class relationship kinds:

- `OWNED`
- `REFERENCED`

Use `OWNED` when the master aggregate lifecycle-manages the satellite.

Use `REFERENCED` when the master only points at an already existing satellite.

### Lifecycle semantics

Lifecycle semantics are domain concepts too:

- `cascadeCreate`
- `cascadeUpdate`
- `cascadeDelete`
- `orphanDelete`
- `hydrateOnFetch`

Kind-based defaults are:

#### `OWNED`

- create: `true`
- update: `true`
- delete: `false`
- orphan delete: `false`
- hydrate on fetch: `true`

#### `REFERENCED`

- create: `false`
- update: `true`
- delete: `false`
- orphan delete: `false`
- hydrate on fetch: `true`

For `REFERENCED`, “update” means:

- the relationship may participate in master update
- this is typically a relink / move operation
- it does **not** mean mutating the referenced aggregate itself

### Reconciliation strategy

`ReconciliationStrategy` matters mainly for `MANY`.

- `REPLACE`
    - the incoming patch describes the new full target set
- `MERGE_BY_ID`
    - the incoming patch describes incremental add/update/remove operations

Typical defaults:

- `ONE` -> `REPLACE`
- `MANY` -> usually `MERGE_BY_ID`

## High-Level Runtime DSL

`cleanCrud` now exposes a high-level runtime DSL that consumes `Relationship` directly.

That means runtime code no longer needs to restate:

- owned vs referenced
- lifecycle semantics
- reconciliation strategy

Those come from `Relationship`.

The public entrypoint is:

- `AggregateRelationshipDefinitions.fromRelationship(...)`

Conceptually:

```java
return AggregateRelationshipDefinitions
        .fromRelationship(
                new PersonRelationships().relationship("notes"),
                noteAggregateCrudDefinition)
        .currentMany(PersonDomainModel::notes)
        .replaceMany((person, notes) -> rebuildPerson(person, notes))
        .build();
```

For `ONE`:

```java
return AggregateRelationshipDefinitions
        .fromRelationship(
                new PersonRelationships().relationship("currentVersion"),
                versionAggregateCrudDefinition)
        .current(person -> Optional.ofNullable(person.currentVersion()))
        .replace((person, currentVersion) -> rebuildPerson(person, currentVersion))
        .build();
```

### Why this DSL exists

It keeps the distinction clear:

- `Relationship` = semantic declaration
- `AggregateRelationshipDefinition` = executable runtime lowering

So the DSL is the bridge from domain meaning to runtime behavior.

## What The Framework Lowers Automatically

For the standard relationship path, `cleanCrud` lowers from `Relationship` into:

- lifecycle semantics
- create input resolver
- patch input resolver
- identity resolver
- link strategy
- hydration strategy
- final `AggregateRelationshipDefinition`

This is the canonical lowering.

That matters because both:

- handwritten runtime wiring
- generator-emitted runtime wiring

now follow the same relationship meaning instead of reinterpreting it separately.

## Current Lowering Conventions

The first pass freezes the current conventions instead of redesigning them.

### Referenced

#### `REFERENCED + ONE`

- create links the provided reference id
- patch relinks to the provided reference id
- remove unlinks when remove ids are present
- no nested satellite mutation

#### `REFERENCED + MANY + MERGE_BY_ID`

- patch adds referenced ids
- patch removes specified ids

#### `REFERENCED + MANY + REPLACE`

- patch replacement collection becomes the new target set

### Owned

#### `OWNED + ONE`

- current generated upsert/remove convention is preserved

#### `OWNED + MANY + MERGE_BY_ID`

- `SatelliteUpdatePatchItem<Id, Patch>` plus remove ids

#### `OWNED + MANY + REPLACE`

- current generated replacement convention is preserved

## What The Generator Now Does

`cleanCrud-generator` now reads:

- the base model
- the sibling `*Relationships` class
- root aggregate id types from generator config

From that, it derives:

- domain relationship fields as `IdentifiedModel<DomainId, DomainModel>` variants
- persistence relationship fields as persistence-id variants using the same property names
- API create / patch / response shapes
- accessors / replacers
- runtime relationship configuration using the new high-level DSL

So the consumer provides:

- the base model
- the relationship declarations

and the generator supplies the structural lowering.

## Example Relationship Output Shape

Generated relationship configuration is now intentionally shorter.

Instead of emitting the full low-level resolver/link/hydration boilerplate, it emits:

- `fromRelationship(...)`
- generated `current(...)` / `currentMany(...)`
- generated `replace(...)` / `replaceMany(...)`

That removes structural duplication from generated code.

## Example Consumer Journey

Suppose you want:

- `Tag` as a standalone aggregate
- `Version` as a standalone aggregate
- `Note` as a standalone aggregate
- `Person` as a standalone aggregate with:
    - `tag` referenced
    - `currentVersion` referenced
    - `lastKnownVersion` referenced
    - `notes` owned

The normal order is:

1. define and wire `Tag`
2. define and wire `Version`
3. define and wire `Note`
4. define `PersonModel<T, V, N>`
5. define `PersonRelationships`
6. generate or wire `Person`

The owning aggregate usually comes last because it points at already existing satellite aggregates.

## Architecture

`cleanCrud` still keeps the same broad layers:

### API

- REST controllers
- application controllers
- facades
- API/domain adapters

### Domain

- models
- DTOs
- builders
- patchers
- response builders
- policies
- validation
- relationship declarations

### Infrastructure

- persistence models
- repositories
- persistence adapters
- ID management

The aggregate runtime sits above persistence services and orchestrates CRUD consistently across those layers.

## Historized Persistence

Historized persistence still follows the same pattern:

1. one live JPA entity
2. one history JPA entity
3. one live repository
4. one history repository
5. one historized repository adapter
6. one history snapshot factory

`AuditActorSupplier` is still the extension point when history rows should capture an actor.

## Example Repository

The companion repository `cleanCrud-sampleImplementation` demonstrates:

- standalone aggregates
- relationship declarations via `*Relationships`
- generated and handwritten runtime wiring
- a real sample where `person` is generated from:
    - `PersonModel`
    - `PersonRelationships`

## Summary

The current `cleanCrud` model is:

- **base model** describes aggregate shape
- **`Relationship`** describes relationship meaning
- **runtime DSL** lowers that meaning into executable aggregate relationship definitions
- **generator** derives the repetitive structural code from those same declarations

That keeps the semantic truth in one place and lets runtime and generation share it instead of drifting apart.
