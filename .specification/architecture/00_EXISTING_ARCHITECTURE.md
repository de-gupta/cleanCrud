# Existing Architecture: A Guided Tour Of `cleanCrud`

## Why This Document Exists

`cleanCrud` is no longer a tiny CRUD helper.
It is a layered application framework with:

- transport adapters
- application controllers and facades
- aggregate definitions
- workflow execution
- relationship orchestration
- an operation umbrella on top of the aggregate substrate
- source-aware operation policy
- post-commit hooks
- durable subprocesses
- creation quarantine
- mutation quarantine

That is a lot of moving pieces.

This document explains the current architecture as a story you can follow from
an incoming request to the final side effects. It is intentionally concrete and
uses the sample implementation as the main narrative anchor, especially the
`Task` aggregate.

It does not focus on method signatures.
It focuses on roles, responsibilities, and the sequence of events.

---

## The Big Picture

Today `cleanCrud` is best understood as one shared aggregate execution substrate
with two business-facing umbrellas on top of it:

1. the **CRUD lane**
2. the **operation umbrella**

The operation umbrella is not a vague future direction.
It already exists in the code under:

- `template.useCases.operation.creation`
- `template.useCases.operation.mutation`

Those two operation families are intentionally parallel.
They are not identical, but they are built from the same architectural idea:

- a typed application payload
- a source-aware request model
- a registry of handlers
- a workflow-oriented aggregate service
- quarantine and replay support
- optional web exposure around quarantine management

CRUD and both operation families share the same aggregate substrate underneath:

- one aggregate definition
- one aggregate fetch port
- one aggregate mutation port
- one workflow engine
- one transaction boundary
- one post-commit dispatch mechanism
- one durable-process start mechanism

They differ mainly in **how business intent is expressed**.

The CRUD lane says:

- save this model
- fetch this aggregate
- put this full replacement at id
- patch this aggregate
- delete this aggregate

The creation lane says:

- here is a typed business command or event that creates a new aggregate
- let the registered handler interpret it into an aggregate creation plan
- apply source-aware policy before persistence
- persist the new aggregate if allowed

The mutation lane says:

- here is a typed business command or event
- load the current aggregate
- let the registered handler interpret it into an aggregate mutation plan
- apply source-aware policy
- persist the resulting state if allowed

So the architecture is not "one CRUD framework plus a random side subsystem".
It is better understood as:

> one aggregate workflow substrate with one CRUD lane and one operation
> umbrella on top of it, where creation and mutation are parallel operation
> families

Another way to say it is:

- CRUD is the direct aggregate manipulation lane
- operation creation is the "typed create intent" lane
- operation mutation is the "typed state transition" lane

---

## The Cast Of Characters

Before following the stories, it helps to know the main actors.

### The aggregate definition

`AggregateCrudDefinition` is the aggregate's contract with the framework.

It tells the framework:

- how to fetch
- how to mutate
- how to build a domain model from create input
- how to patch a domain model
- how to build a response
- what insertion / patch / deletion rules apply
- what security rules apply
- what duplicate rules apply
- what post-commit hook should run
- what relationships exist
- what source-aware operation policy profiles apply

This is the center of gravity of the architecture.
If the framework needs to know "how does this aggregate behave?", it asks the
aggregate definition.

### The aggregate ports

The aggregate definition does not talk directly to repositories.
It exposes:

- `AggregateFetchPort`
- `AggregateMutationPort`

These are the clean runtime seams between orchestration and persistence.

In the sample repo, these ports are usually built from lower persistence
services using:

- `AggregateFetchPortAdapter`
- `AggregateMutationPortAdapter`

### The workflow engine

`AggregateLifecycleEngine` is the shared executor.

Its concrete implementation, `DefaultAggregateLifecycleEngine`, owns:

- the transaction boundary through `PersistenceTransactionRunner`
- post-transaction dispatch
- durable process task start
- immediate durable-process execution nudge
- creation quarantine recording access
- mutation quarantine recording access

Importantly, the engine does **not** decide the business sequence of "save",
"patch", "delete", "create", or "mutate". The services define those workflows.
The engine executes them.

### The workflow object

The services package their work into a `CrudWorkflow`.

A workflow says, conceptually:

- what happens in the transaction
- what happens after the transaction
- what durable process start requests should be emitted
- whether the workflow is read-only

`CrudWorkflowBuilder` exists to make those workflow objects readable and named:

- `writeFlow(...)`
- `readOnlyFlow(...)`
- `afterTransaction(...)`
- `startDurableProcesses(...)`

### The CRUD services

The CRUD lane is implemented by four parallel abstract services:

- `AbstractSaveService`
- `AbstractFetchService`
- `AbstractUpdateService`
- `AbstractDeleteService`

These are peers.
Each one defines the business sequence for its own use case family, then hands
that sequence to the engine as a workflow.

`AggregateCrudServices` is the convenience factory that instantiates these
framework services for a concrete aggregate.

### The mutation service

The mutation lane is implemented by:

- `AggregateMutationServices`
- `DefaultAggregateMutationService`
- `QuarantinableMutationService`

This service is the mutation equivalent of the CRUD services.
It is application-facing, workflow-based, and aggregate-aware.

### The creation service

The creation side of the operation lane is implemented by:

- `AggregateCreationServices`
- `DefaultAggregateCreationService`
- `QuarantinableCreationService`

This service is the creation-family sibling of the mutation service.
It is also application-facing, workflow-based, and aggregate-aware, but starts
from a typed creation payload instead of an existing aggregate id.

### Relationship coordinators

When an aggregate has owned or referenced satellites, orchestration is pushed
into dedicated helpers instead of being smeared across the engine:

- `AggregateSaveCoordinator`
- `AggregateFetchCoordinator`
- `AggregateUpdateCoordinator`
- `AggregateDeleteCoordinator`
- `AggregateMutationCoordinator`

These are where relationship-aware execution lives.

They are not interchangeable helpers.
Each coordinator owns one use-case family's relationship semantics.

`AggregateSaveCoordinator` is the create-time coordinator.
It starts from a root model that has already been built by the normal create
builder and then:

- resolves satellite create intents through `SatelliteCreateIntentResolver`
- decides whether each relationship must be handled before or after root
  persistence based on `SatellitePersistenceOrder`
- links resolved satellite ids back into the root through
  `SatelliteRelationshipPlanner`
- persists the root
- persists a relinked root one more time if after-persistence relationships
  changed the linked ids

So save coordination is mostly about creating or resolving satellites and
linking the correct ids into the root at the correct time.

`AggregateFetchCoordinator` is the read coordinator.
It first applies security visibility filtering and then, for each relationship
whose lifecycle semantics say `hydrateOnFetch`, invokes the relationship's
hydration strategy.
That is how an identified root that only stores satellite ids becomes a
hydrated aggregate view on the way out.

`AggregateUpdateCoordinator` is the most complex one because update
reconciliation is genuinely rich.
It owns:

- `put` replacement semantics for aggregates with relationships
- patch semantics for aggregates with relationships
- satellite create / reference / update / remove intent execution
- reconciliation strategy handling such as `REPLACE` vs `MERGE_BY_ID`
- orphan deletion handling
- validation of "current satellite" shorthand operations for one-vs-many
  cardinality
- source-aware patch validation for both root and owned satellites

In practice it compares current linked ids, target linked ids, and mutation
intents, then decides which satellites to keep, create, update, unlink, or
delete.
That is why it is the largest CRUD coordinator.

`AggregateDeleteCoordinator` is the teardown coordinator.
It validates root deletion, walks relationships that allow `cascadeDelete`,
resolves currently linked satellites, validates each satellite's deletion
policy, deletes satellites first, and only then deletes the root.

`AggregateMutationCoordinator` is the mutation-lane analogue.
It does not start from a patch DTO.
It starts from the richer `AggregateMutationPlan` produced by a mutation
handler and makes that plan executable against owned relationships.

Another important detail is where these coordinator instances come from.
They are not Spring beans.
They are stateless framework runtime helpers created once in
`AggregateServiceSupportFactory`, alongside:

- `AggregateDefinitionGuard`
- `AggregateMutationValidationSupport`
- `SatelliteRelationshipPlanner`
- `SatelliteReferenceResolver`
- `SatelliteCreateIntentResolver`

That is deliberate.
These objects are framework-internal reusable machinery, not application
assembly points that consumers are expected to inject per module.

### Validation and policy support

Three different kinds of rule checking matter:

1. classic CRUD validation
2. source-aware creation policy
3. source-aware mutation policy

Classic CRUD validation is mainly handled by:

- `AggregateMutationValidationSupport`

Source-aware creation policy is built by:

- `AggregateCreationPolicies`

Source-aware mutation policy is built by:

- `AggregateMutationPolicies`

Those policy bundles combine:

- access policy
- transition policy
- domain invariant policy
- external consistency policy
- a source-specific handling profile

### Post-commit and durable work

There are two "after the main commit" concepts:

1. lightweight `postCommitMutation(...)`
2. durable subprocesses

The lightweight hook is fire-and-forget best effort.
The durable subprocess path persists a task and then nudges execution.

These are intentionally separate.

### Operation quarantine

The operation lane can decide that a creation or mutation should neither be
accepted nor simply rejected. It can be **quarantined**.

That means:

- the aggregate change is not applied
- the operation request is persisted
- it can later be inspected and replayed

The two operation families currently have their own quarantine subsystems:

- `template.useCases.operation.creation.quarantine`
- `template.useCases.operation.mutation.quarantine`

But they already share some operation-level concepts.
Both quarantine records now carry:

- an `aggregateType`
- an operation family and source
- correlation and causation ids
- replay attempt metadata
- `QuarantineReplayOutcome`

Both sides also persist replayable envelopes rather than only transient request
objects, and the web responses deliberately expose the payload type name instead
of dumping arbitrary payload JSON back to clients.

#### Creation quarantine

The creation lane can quarantine a proposed creation before root persistence or
while relationship-aware creation is being resolved.

That means:

- the aggregate is not created
- the creation request is persisted
- it can later be inspected, dismissed, or replayed

This is implemented by the creation quarantine subsystem and wired into the
engine through `CreationQuarantineRecorder`.

#### Mutation quarantine

The mutation lane can decide that a mutation should neither be accepted nor
simply rejected. It can be **quarantined**.

That means:

- the aggregate is not updated
- the mutation request is persisted
- it can later be inspected and replayed

This is implemented by the mutation quarantine subsystem and wired into the
engine through `MutationQuarantineRecorder`.

---

## Story One: A Normal CRUD Save Request

Let us walk through a concrete example:

> a client sends `POST /task/save` to create a `Task`

We will follow the sample implementation.

### Scene 1: The request hits the REST controller

The first Spring bean involved is:

- `TaskSpringRestSaveController`

This class is tiny by design.
It mainly declares:

- this is a REST controller
- it lives at `/task/save`
- it belongs to the "Task Save" OpenAPI group

It inherits almost all actual request handling behavior from
`AbstractSpringRestSaveController`.

This is the first recurring architectural pattern in `cleanCrud`:

> handwritten module classes are usually very small because the generic
> behavior lives in abstract framework base classes

The controller does not know how to persist a task.
It only knows who to delegate to next.

### Scene 2: The application controller receives the API model

The REST controller delegates to:

- `TaskSaveApplicationController`

This is still transport-adjacent, but one step more abstract.
Its job is to say:

- this is now an application save request
- delegate to the save facade

The application controller is still API-shaped.
It works with `TaskAPIModelCreate` and `TaskAPIModelResponse`.

### Scene 3: The facade translates transport language into domain language

The next actor is:

- `TaskSaveServiceFacade`

This is where the transport model stops being the center of the conversation.
The facade uses:

- `TaskAPIToDomainCreateAdapter`
- `TaskDomainToAPIResponseAdapter`

So the request shape is translated like this:

1. API create DTO becomes domain create DTO
2. the domain service is called
3. the domain response is mapped back to API response DTO

This is an important design choice.
The framework does not let REST DTOs leak straight into the aggregate runtime.

### Scene 4: The facade calls the framework save service

Under the facade lives a framework `SaveService`.

In the sample repo this service bean is created in:

- `TaskCrudServicesConfiguration`

That class calls:

- `AggregateCrudServices.saveService(...)`

This is where the sample module stops manually assembling behavior and starts
asking the framework to do what it is good at.

It also shows one of the recurring bean-creation patterns in the repo.
The sample module does not subclass `AbstractSaveService`.
Instead, `TaskCrudServicesConfiguration` creates a `SaveService` bean through
`AggregateCrudServices.saveService(...)`.

Why a `@Bean` factory method here instead of a handwritten concrete class?

- the framework service already exists and is generic
- the module only needs to provide the aggregate definition and engine
- optional durable subprocess wiring is easier to read in a short config method
- qualifiers can name the aggregate-specific bean explicitly

So in the CRUD lane the usual rule is:

- domain-specific leaf behavior becomes a small concrete class
- generic framework assembly becomes a `@Bean` in a configuration class

The save service is created from:

- the aggregate definition
- the aggregate lifecycle engine
- optionally a function that maps saved models to durable process start requests

For `Task`, save is interesting because it also starts a durable subprocess for
printing after creation.

### Scene 5: The aggregate definition provides the rules of the game

The save service does not carry aggregate-specific knowledge inside itself.
It asks the `Task` aggregate definition for that knowledge.

That definition is built in:

- `TaskCrudDefinitionConfiguration`

The definition includes:

- the fetch and mutation ports
- the create builder
- the patcher
- the response builder
- insertion / patch / deletion policies
- security policy
- duplicate definition
- post-commit mutation hook
- relationship definitions for `version` and `note`

This is why `AggregateCrudDefinition` is so central.
The save service is generic; the definition makes it a `Task` save service.

This is another intentional `@Bean` assembly point.
`TaskCrudDefinitionConfiguration` does not represent new business behavior.
It gathers already-existing pieces:

- aggregate ports
- builders
- policies
- duplicate definition
- post-commit hook
- relationship definitions

and freezes them into one aggregate contract bean.

So the repo prefers declarative definition assembly over a big handwritten
`TaskAggregateCrudDefinition` class.

### Scene 6: The aggregate ports bridge into persistence

The aggregate definition does not know repositories either.
It points at:

- `taskAggregateMutationPort`
- `taskAggregateFetchPort`

Those are created in:

- `TaskCrudPortsConfiguration`

The mutation port is built from the three persistence services:

- save persistence
- update persistence
- delete persistence

The fetch port is built from the fetch persistence service.

This means the orchestration layer speaks in terms of aggregate fetch/mutation,
not raw JPA repositories.

### Scene 7: The save service builds a workflow

Now we are inside `AbstractSaveService`.

Its job is to define the business sequence for save.
It does this by:

1. asking `AggregateDefinitionGuard` for executable relationship definitions
2. building a `CrudWorkflow`
3. handing that workflow to the engine

Conceptually, the workflow says:

- inside the transaction: persist the new tasks
- also inside the transaction: register durable process tasks if requested
- after the transaction: dispatch `postCommitMutation` with `CREATE` semantics

That is a clean separation:

- **service** decides the workflow
- **engine** executes the workflow

### Scene 8: The service chooses between simple and relationship-aware save

Inside the transactional part, `AbstractSaveService` asks:

- does this aggregate have executable relationships?

If not, the flow is simple:

1. create domain models through the aggregate's create builder
2. validate duplicates, access, and insertion policy
3. call `mutationPort.create(...)`

If yes, the flow becomes relationship-aware:

1. still validate the would-be root models
2. delegate to `AggregateSaveCoordinator`

This is where `Task` becomes interesting, because `Task` has:

- a `version` relationship
- a `note` relationship

### Scene 9: Relationship configuration has already lowered domain meaning into executable behavior

The `Task` relationships are declared in:

- `TaskCrudRelationshipConfiguration`

This class lowers relationship semantics into executable definitions using
`AggregateRelationshipDefinitions`.

For `Task`, that configuration tells the framework things like:

- `version` is a one-to-one satellite
- `note` is a one-to-many satellite
- they should cascade create
- they should cascade update
- they should cascade delete
- orphan delete is allowed
- fetch should hydrate them
- these are the extractors, replacers, and mappers needed to move between
  `Task` and the satellite aggregates

This is a core architectural idea:

> relationship meaning is declared once, then lowered into a runtime structure
> that the coordinators can execute

### Scene 10: The coordinator performs relationship-aware persistence

`AggregateSaveCoordinator` is where the relationship-aware save actually
happens.

Concretely, its sequence is:

1. take the root model already produced by the aggregate create builder
2. walk executable relationships whose persistence order is
   `SATELLITE_BEFORE_MASTER`
3. resolve or create the intended satellites for those relationships
4. link their ids into the root through `SatelliteRelationshipPlanner`
5. validate and persist the root
6. walk relationships whose persistence order is
   `SATELLITE_AFTER_MASTER`
7. resolve or create those satellites now
8. relink their ids into the persisted root
9. if the relinked root is a different object, write the root back once more

The two important lower-level collaborators are:

- `SatelliteCreateIntentResolver`, which turns relationship input into
  concrete satellite ids
- `SatelliteRelationshipPlanner`, which knows how to read and replace linked
  satellite ids on the root model

So the coordinator does not know anything hard-coded about `Task`, `Version`,
or `Note`.
It executes the runtime relationship definition model.

The architectural point is:

- the save service owns the workflow shape
- the coordinator owns the relationship-specific persistence choreography

This keeps the engine small and the service legible.

### Scene 11: The engine executes the workflow inside a transaction

At this point the workflow reaches `DefaultAggregateLifecycleEngine`.

The engine does not know that this is `Task`.
It only knows:

- run `inTransaction()`
- collect any durable process start requests returned from the workflow result
- start those durable process tasks while still in the transaction
- after commit, dispatch the workflow's `afterTransaction(...)`
- after that, nudge durable process execution

The transaction is provided by:

- `PersistenceTransactionRunner`

In the sample repo, `CommonPersistenceConfiguration` wires a
`SpringPersistenceTransactionRunner` and builds the engine with:

- durable process starter
- durable process execution nudge
- creation quarantine recorder
- mutation quarantine recorder

So this single engine bean is the shared runtime substrate for both CRUD and
the application operation lane.

This engine bean is created through an explicit `@Bean` method in
`CommonPersistenceConfiguration`, not through component scanning.
That choice matters.

`DefaultAggregateLifecycleEngine` is infrastructure assembly, not leaf domain
behavior.
It needs several cross-cutting collaborators:

- transaction runner
- durable process starter
- durable process execution nudge
- creation quarantine recorder
- mutation quarantine recorder

Putting that assembly in one configuration method makes the runtime wiring
visible and keeps the framework less magical.

### Scene 12: The post-commit hook runs after the main save

The `Task` aggregate definition installs a lightweight post-commit hook that
prints:

- mutation kind
- task id

This is exposed through `definition.postCommitMutation()`.

For save, `AbstractSaveService` dispatches it with `CREATE` semantics using a
`PostCommitMutationContext`.

That hook is best-effort and asynchronous.
It is intentionally small and non-durable.

### Scene 13: The durable subprocess is also started

The `Task` save service additionally maps saved tasks into durable process
requests using:

- `taskPrintStartRequests(...)`

That produces `DurableProcessStartRequest` objects.

The engine starts those durable tasks inside the main transaction.
Then, after the transaction commits, it nudges the durable process subsystem to
run the newly created tasks immediately.

This is the key distinction:

- post-commit hook: lightweight, best-effort, asynchronous
- durable subprocess: persisted, retryable, recoverable

### Scene 14: The response climbs back up the stack

Once the save service returns identified domain responses, the stack unwinds:

1. save service returns identified domain response DTOs
2. facade maps them into API response DTOs
3. application controller returns them
4. REST controller writes them out as HTTP response

By the time the response leaves the process, the system may also have:

- printed the post-commit log line
- persisted a durable subprocess task
- already nudged that task for immediate execution

That is the full end-to-end save story.

---

## CRUD In The Other Three Cases

Once you understand save, the other CRUD families are variations on the same
theme.

### Fetch

`AbstractFetchService` defines read-only workflows.

Its sequence is:

1. resolve executable relationships
2. if no relationships exist:
    - fetch directly through the fetch port
    - apply security visibility filtering
3. if relationships exist:
    - delegate to `AggregateFetchCoordinator`
    - hydrate satellites

So fetch is the read mirror of save.

More concretely, `AggregateFetchCoordinator` does two things in the
relationship-aware branch.

First, it applies visibility filtering.
If the aggregate's security policy says a model is not visible, collection
results drop it and single-object lookup behaves as not found.

Second, it performs hydration.
For each configured relationship whose lifecycle semantics say
`hydrateOnFetch`, it invokes that relationship's hydration strategy with:

- the identified root
- the satellite fetch port
- the relationship link strategy

That is how a root that only stores linked satellite ids becomes a richer
aggregate view on the way out.

### Update

`AbstractUpdateService` defines three update flavors:

- `putAtId`
- `updateById`
- `updateAllById`

Its sequence is:

1. resolve relationships
2. load the current model where needed
3. for non-relationship aggregates:
    - `put` uses create-builder replacement semantics
    - `patch` uses the aggregate patcher
    - validate access and patch rules
    - persist through mutation port
4. for relationship aggregates:
    - delegate to `AggregateUpdateCoordinator`
5. build `PostCommitMutationContext` with `PUT` or `PATCH`
6. optionally start durable subprocesses
7. dispatch post-commit work after commit

Bulk update supports two semantics:

- `ALL_OR_NOTHING`
- `BEST_EFFORT`

That is a good example of why the services own business sequencing and not the
engine. Bulk semantics belong to the use case, not to the executor.

The relationship-aware branch is where `AggregateUpdateCoordinator` earns its
size.
It has to preserve the exact semantics of:

- `put` as replacement
- patch as incremental mutation
- relationship reconciliation strategy
- orphan deletion
- shorthand "current satellite" operations

For `put`, it computes the new target satellite set for the replacement and
removes orphaned satellites where lifecycle semantics allow it.

For patch, it reads mutation intents from the relationship planner and executes
them one by one.
Typical intent kinds are:

- reference an existing satellite
- create a new satellite
- update a linked satellite
- update the current linked satellite
- upsert the current linked satellite
- remove a linked satellite
- remove the current linked satellite

Then, depending on reconciliation strategy, it either builds a completely new
target set in order or merges the changes into the current set by id.

It also validates cardinality-sensitive shortcuts.
For example, "update current satellite" is valid for a one-to-one
relationship, but ambiguous for one-to-many and therefore rejected there.

### Delete

`AbstractDeleteService` mirrors update:

1. resolve relationships
2. fetch the current aggregate
3. validate deletion rules
4. if no relationships exist:
    - delete directly through mutation port
5. if relationships exist:
    - delegate to `AggregateDeleteCoordinator`
6. build `DELETE` post-commit context
7. optionally start durable subprocesses
8. dispatch post-commit after commit

Bulk delete also supports:

- `ALL_OR_NOTHING`
- `BEST_EFFORT`

So the CRUD lane is internally very regular:

- four peer services
- each defines its own workflow
- all execute through the same engine

`AggregateDeleteCoordinator` is simpler than update, but it still owns real
aggregate semantics.
It is not just "delete root and hope".

It walks each relationship and asks whether lifecycle semantics allow
`cascadeDelete`.
If yes, it resolves the currently linked satellites, validates each satellite's
deletion policy, deletes satellites first, and only then deletes the root.
That ordering is what makes aggregate delete honor ownership semantics.

---

## Story Two: A Creation Request

Now let us switch from CRUD to the application operation lane's creation
family.

The sample repo demonstrates this with typed creation payloads such as:

- `RegisterTagCreation`
- `RegisterTaskCreation`

These are handled through the creation operation lane, not through REST save
controllers.

### Scene 1: Someone calls the creation application controller

The entry point is:

- `CreationApplicationController`

Like the mutation controller, it exposes source-aware methods such as:

- `applyUserIntent(...)`
- `applyInternalCommand(...)`
- `applyAuthoritativeExternalEvent(...)`
- `applyProcessEmittedAction(...)`

So operation creation is not modeled as "just call save from another
controller". It is its own application surface with explicit source semantics.

### Scene 2: The controller packages the call as a `CreationRequest`

The controller builds a `CreationRequest`.

A `CreationRequest` conceptually carries:

- typed payload
- operation source
- operation family
- optional correlation id
- optional causation id

This gives operation creation richer semantics than plain CRUD save.

### Scene 3: The creation service dispatches through a handler registry

The creation lane is implemented by:

- `AggregateCreationServices`
- `DefaultAggregateCreationService`

The service asks a `CreationHandlerRegistry` to find the registered handler for
the incoming payload type.

That handler may return:

- a simple create input
- or an `AggregateCreationPlan`

So the creation lane is open for additional intent-shaped creation use cases
without changing the service itself.

### Scene 4: The service evaluates source-aware creation policy

Once the would-be root model has been built, the creation service applies:

- `AggregateCreationPolicies.sourceAwarePolicy(definition)`

This is the creation-family sibling of mutation policy.
It can:

- allow creation
- quarantine creation
- surface tolerated violations in the returned `CreationResult`

So operation creation is semantically richer than CRUD save even though both
eventually reuse the same aggregate infrastructure.

### Scene 5: Relationship-aware creation reuses the CRUD aggregate substrate

If the aggregate has no executable relationships, the service can persist the
root directly through the mutation port.

If it does have owned relationships, `DefaultAggregateCreationService`
delegates to the same `AggregateSaveCoordinator` used by CRUD save.

That is one of the central architectural choices of the operation lane:

- application semantics stay distinct
- aggregate persistence choreography is reused underneath

So operation creation is not a parallel persistence implementation.
It is a parallel application entry lane on top of the same aggregate save
machinery.

### Scene 6: The result is explicitly semantic

The creation lane returns a `CreationResult`, not just an identified model.

That result can carry:

- `CreationContext`
- policy decision
- optional `CreateResult`
- optional quarantine request

So callers can choose between convenient unwrapping and preserving the richer
semantic result.

### Scene 7: Quarantine and replay are first-class

If source-aware creation policy says quarantine:

1. the aggregate is not created
2. the creation request is persisted through `CreationQuarantineRecorder`
3. the service returns `CreationResult.quarantined(...)`

Later, the creation quarantine subsystem can replay that request back through
the normal creation service using source:

- `ADMINISTRATIVE_REPLAY`

So creation now mirrors mutation in having durable quarantine persistence and
replay support.

---

## Story Three: A Mutation Request

Now let us switch lanes.

This time the concrete example is:

> the application wants to annotate a task with a new title and an owned note

In the sample repo this mutation is modeled as:

- `AnnotateTaskMutation`

and handled by:

- `AnnotateTaskMutationHandler`

Unlike CRUD, this path is currently **application-first**, not REST-first.
That is intentional.

### Scene 1: Someone calls the mutation application controller

The entry point is:

- `MutationApplicationController`

For `Task`, the sample config creates:

- `taskMutationApplicationController`

through:

- `TaskMutationConfiguration`

This controller exposes methods like:

- `applyUserIntent(...)`
- `applyInternalCommand(...)`
- `applyAuthoritativeExternalEvent(...)`
- `applyProcessEmittedAction(...)`

So unlike CRUD, the mutation lane begins with an explicit declaration of
**source semantics**.

That is one of its most important architectural ideas.

### Scene 2: The controller packages the call as a `MutationRequest`

The mutation application controller is thin.
Its main job is to build a `MutationRequest` and delegate to the mutation
service.

A `MutationRequest` conceptually carries:

- aggregate id
- typed payload
- mutation source
- mutation family
- optional correlation id
- optional causation id

This makes mutation requests richer than CRUD patch calls.
CRUD patching mostly says "apply this patch".
Mutation says "apply this business fact or command from this source".

### Scene 3: The mutation service is created from the same aggregate definition

`TaskMutationConfiguration` wires:

- a registered handler
- a mutation handler registry
- a `MutationService`
- a `MutationApplicationController`

The actual service is produced by:

- `AggregateMutationServices.mutationService(...)`

And notice what it needs:

- the same `taskAggregateCrudDefinition`
- the same `AggregateLifecycleEngine`
- a mutation handler registry

This is the architectural proof that the mutation lane is a sibling of CRUD,
not a separate application world.

It also shows the same bean-creation pattern as CRUD.
The sample module does not define a handwritten `TaskMutationService`.
Instead, `TaskMutationConfiguration` creates:

- a registered handler bean
- a mutation handler registry bean
- a framework mutation service bean
- a framework mutation application controller bean

The domain-specific behavior lives in the handler.
The service and controller are generic framework machinery, so a factory bean
method is clearer than subclassing.

### Scene 4: The handler registry dispatches by payload type

The mutation service does not contain a hard-coded `switch` over payload
classes.
Instead it asks:

- `MutationHandlerRegistry`

For `Task`, the registry contains:

- `taskAnnotateMutationHandler`

registered against:

- `AnnotateTaskMutation.class`

That makes the mutation lane open for extension without changing the service
itself.

### Scene 5: The mutation service builds a write workflow

Inside `DefaultAggregateMutationService`, the request becomes another workflow.

Conceptually, the mutation workflow says:

- inside the transaction:
    - load the current aggregate
    - apply the registered handler
    - apply relationship mutations if present
    - evaluate source-aware policy
    - either quarantine or persist
- still inside the transaction:
    - start any durable subprocesses requested from the mutation context
- after the transaction:
    - emit a patch-shaped post-commit mutation if the mutation succeeded

So the mutation lane reuses the same engine idea as CRUD:

- service defines the workflow
- engine executes it

### Scene 6: The service loads the current aggregate

The mutation lane is always state-relative.
It starts by fetching the current aggregate from:

- `definition.fetchPort()`

If the id does not exist, the mutation cannot proceed.

This is different from save.
Save is about creating something new.
Mutation is about interpreting a command or event in the context of current
state.

### Scene 7: The registered handler interprets the business payload

Now the important part:

- `AnnotateTaskMutationHandler`

This handler is not just a patcher.
It is allowed to express richer intent.

For `Task`, it does two things:

1. it builds an updated root `TaskDomainModel` with a new title
2. it adds a relationship mutation plan for the `note` relationship

It returns an `AggregateMutationPlan`.

This is the key mutation-lane abstraction.
The handler says:

- here is the new root state, if any
- here are the intended satellite mutations, if any

That makes the mutation lane expressive enough to mutate both:

- the aggregate root
- owned satellites

without forcing everything through a REST patch DTO.

### Scene 8: The mutation coordinator applies relationship participation

If the aggregate has no relationships, the updated root can be used directly.

If it does have relationships, `DefaultAggregateMutationService` delegates to:

- `AggregateMutationCoordinator`

This class is the mutation-lane analogue of the CRUD relationship coordinators.
Its job is to take the `AggregateMutationPlan` and reconcile it against the
actual aggregate relationship model.

Important current semantics:

- owned relationships can participate
- referenced relationships may not be mutated from this lane
- mutation semantics respect lifecycle semantics and reconciliation strategy

So if a user wants to mutate a referenced aggregate through another aggregate,
the framework explicitly rejects that and tells them to mutate the referenced
aggregate directly.

That is a good architectural boundary.

The reason `AggregateMutationCoordinator` exists separately from
`AggregateUpdateCoordinator` is that the mutation lane starts from different
input.
CRUD update begins with create DTOs or patch DTOs.
Mutation begins with a typed command or event that a handler turns into an
`AggregateMutationPlan`.

So the mutation coordinator's job is:

- take the handler-produced root change and relationship mutation plan
- validate that only supported relationship kinds participate
- reconcile that plan against the current aggregate relationship state
- return the executable updated aggregate model that policy evaluation will
  inspect

The handler expresses business intent.
The coordinator makes that intent executable against the aggregate runtime
model.

### Scene 9: Source-aware policy evaluates the proposed state change

After the updated aggregate state is computed, the mutation service runs the
source-aware policy.

That policy is constructed by:

- `AggregateMutationPolicies.sourceAwarePolicy(definition)`

The policy bundle combines:

- mutation access policy
- mutation transition policy
- domain invariant policy
- external consistency policy
- mutation policy profile resolver

The profile resolver decides, based on source, how different violations should
be handled:

- allowed
- rejected
- quarantined

This is where the mutation lane becomes meaningfully different from CRUD patch.

CRUD patch mostly applies:

- access
- patch validity

Mutation can distinguish:

- user intent
- internal command
- authoritative external event
- process-emitted action
- administrative replay

And it can react differently to the same violation depending on source.

The `Tag` sample is the clearest example of this.
Its `TagMutationPolicyConfiguration` allows and quarantines different things for
different sources.

### Scene 10: The mutation either succeeds or becomes quarantined

Once policy is evaluated, there are two main outcomes.

#### Success

If the policy allows the mutation:

1. the mutation port updates the aggregate
2. the workflow returns a `MutationResult.applied(...)`

#### Quarantine

If the policy says quarantine:

1. the aggregate is not updated
2. the mutation request is persisted through the engine's
   `MutationQuarantineRecorder`
3. the workflow returns a `MutationResult.quarantined(...)`

This is a powerful design point.
The framework does not collapse every violation into success or failure.
It can preserve suspicious or externally inconsistent events for later review.

### Scene 11: The mutation result carries rich context

A `MutationResult` includes:

- `MutationContext`
- policy decision
- optional updated identified model

The context carries:

- domain id
- source
- family
- payload type
- correlation id
- causation id
- before model
- after model

That gives the mutation lane stronger observability and replay semantics than
plain CRUD update.

### Scene 12: Post-commit still looks patch-shaped

If the mutation succeeds, `DefaultAggregateMutationService` dispatches the
aggregate's normal `postCommitMutation(...)` hook with:

- `PostCommitMutationKind.PATCH`

This is a deliberate simplification in the current architecture.
From the post-commit hook's point of view, a successful mutation is expressed as
"a patch-like state transition happened".

That keeps the post-commit surface small even though the mutation lane is
richer internally.

### Scene 13: Replay is built in

`DefaultAggregateMutationService` also implements:

- `MutationQuarantineReplayGateway`

That means the same service can replay a quarantined mutation later through the
normal mutation path, but with source:

- `ADMINISTRATIVE_REPLAY`

This is an elegant architectural choice:

- the quarantine system does not need to know how to re-run business logic
- it delegates replay back to the aggregate mutation service that already owns
  the logic

### Scene 14: The result returns to the caller

If the caller used:

- `applyInternalCommand(...)`

then a successful `MutationResult` becomes an updated identified domain model.

If the caller used:

- `applyInternalCommandWithResult(...)`

they get the richer result object, including tolerated violations or quarantine
information.

So the mutation application controller gives consumers a choice:

- convenient simple API
- or full semantic result

---

## How CRUD And Operation Relate

By now the family resemblance should be clear.

CRUD and both operation families:

- are aggregate-based
- use the same aggregate definition
- use the same fetch and mutation ports
- execute through the same workflow engine
- run inside the same transaction boundary
- can trigger the same post-commit hook
- can start the same durable subprocesses

Creation and mutation also resemble each other much more than either resembles
plain CRUD.

Both operation families currently have:

- an application controller
- a request object carrying source, family, payload type, and correlation data
- a handler registry keyed by payload class
- a default aggregate service created by a small service factory
- source-aware policy evaluation
- quarantine recording through the shared lifecycle engine
- replay routed back through the same aggregate service via
  `ADMINISTRATIVE_REPLAY`

But they still differ in where their business flow starts.

### CRUD says

- here is create input
- here is patch input
- here is delete intent

### Operation creation says

- here is a typed business creation payload
- here is where it came from
- here is the handler that knows how to interpret it
- here is the policy profile that determines whether to allow or quarantine it
- if allowed, create a new aggregate, optionally through relationship-aware save

### Mutation says

- here is a business payload
- here is the aggregate id whose current state matters
- here is where it came from
- here is the handler that knows how to interpret it
- here is the policy profile that determines what to do with violations
- if allowed, apply the resulting state transition to the existing aggregate

So the current architecture already hints at the broader direction:

> CRUD is one business lane on top of a general aggregate workflow substrate.
> The operation umbrella is another, and creation plus mutation are its two
> parallel use-case families in the current implementation.

---

## The Supporting Subsystems

The two big stories above make more sense once the supporting subsystems are
named explicitly.

### Durable process subsystem

The durable process infrastructure is auto-configured by
`DurableProcessInfrastructureAutoConfiguration`.

It provides:

- durable task persistence
- task scheduler lookup
- durable process starter
- durable process runner
- immediate execution nudge
- polling scheduler
- default no-op action dispatcher unless the consumer provides one

The important architectural relation is this:

- CRUD, operation creation, or mutation can start a durable task
- the engine persists that task in the same transaction
- after commit, the engine nudges execution
- if execution fails or must retry, the durable subsystem owns that lifecycle

So durable processes are not inside CRUD or the operation families.
They are a separate subsystem that both lanes can use.

### Creation quarantine subsystem

The creation quarantine infrastructure is auto-configured by
`CreationQuarantineInfrastructureAutoConfiguration`.

It provides:

- creation quarantine repository
- creation quarantine service
- recorder bean
- replay registry
- application controller
- optional REST controller

The relation to the creation lane is:

- creation service decides whether to quarantine
- engine exposes the recorder
- quarantine subsystem persists, inspects, dismisses, and later replays
- replay gateway discovery is keyed by aggregate type through
  `CreationQuarantineReplayGateway`

Again, this is a separate subsystem, but tightly integrated with the creation
lane.

### Mutation quarantine subsystem

The mutation quarantine infrastructure is auto-configured by
`MutationQuarantineInfrastructureAutoConfiguration`.

It provides:

- mutation quarantine repository
- mutation quarantine service
- recorder bean
- replay registry
- application controller
- optional REST controller

The relation to the mutation lane is:

- mutation service decides whether to quarantine
- engine exposes the recorder
- quarantine subsystem persists, inspects, dismisses, and later replays
- replay gateway discovery is keyed by aggregate type through
  `MutationQuarantineReplayGateway`

Again, this is a separate subsystem, but tightly integrated with the mutation
lane.

### Common persistence configuration in the sample app

The sample app's `CommonPersistenceConfiguration` is the place where the shared
runtime is assembled.

It wires:

- `PersistenceTransactionRunner`
- `AggregateLifecycleEngine`

And the engine is built with:

- durable process starter
- durable process execution nudge
- creation quarantine recorder
- mutation quarantine recorder

That one bean is a good summary of the modern architecture.
The engine is not just "run this in a transaction".
It is the shared lifecycle executor for:

- CRUD
- operation creation
- mutation
- durable-process start
- creation quarantine recording access
- mutation quarantine recording access

This is also a good place to make the bean taxonomy explicit.
The sample app uses three main ways of getting Spring beans.

### 1. Component-scanned concrete classes

These are the leaf classes where the module is expressing its own behavior:

- REST controllers
- facades
- API/domain adapters
- persistence services
- creation handlers
- mutation handlers

These classes are usually small, named, and domain-specific.
They earn their own class because the class name itself carries module meaning.

### 2. `@Configuration` plus `@Bean` assembly methods

These are used when the module is composing generic framework machinery:

- aggregate definition beans
- aggregate fetch/mutation port beans
- CRUD service beans
- creation service beans
- mutation service beans
- creation application controller beans
- mutation application controller beans
- relationship definition beans
- the shared lifecycle engine bean

These objects are often:

- generic
- qualifier-sensitive
- heavily parameterized
- clearer as declarative assembly than as subclasses

So the repo often prefers a config method that says:

- "given this definition and this engine, produce a save service"

instead of a custom concrete class that mostly forwards to the framework.

### 3. Framework auto-configuration

Some infrastructure is created by `cleanCrud` itself through Spring
auto-configuration classes, such as:

- durable process infrastructure
- creation quarantine infrastructure
- mutation quarantine infrastructure
- optional Swagger support

These beans exist because the framework wants to provide batteries-included
runtime infrastructure once the right dependencies are present.

So the practical rule of thumb is:

- domain behavior lives in small concrete classes
- module assembly lives in handwritten configuration classes
- reusable infrastructure lives in framework auto-configuration

---

## The Mental Model That Makes The Code Readable

If you try to read this repository as "controllers call services and services
call repositories", it will feel too large and too abstract.

A better mental model is:

### Layer 1: Transport and translation

These classes speak API language:

- Spring REST controllers
- application controllers
- API/domain adapters
- facades

Their job is to move data in and out cleanly.

### Layer 2: Aggregate use-case services

These classes decide the business sequence:

- `AbstractSaveService`
- `AbstractFetchService`
- `AbstractUpdateService`
- `AbstractDeleteService`
- `DefaultAggregateCreationService`
- `DefaultAggregateMutationService`

Their job is to define workflows.

### Layer 3: Aggregate runtime substrate

These classes execute and support workflows:

- `AggregateLifecycleEngine`
- `CrudWorkflow`
- validation support
- relationship coordinators
- aggregate ports

Their job is to provide reusable orchestration machinery.

### Layer 4: Persistence and infrastructure

These classes actually move data and side effects:

- persistence services
- repositories
- JPA models
- durable process storage and runner
- creation quarantine storage and replay
- mutation quarantine storage and replay

Their job is to make the workflows real.

If you read the repo with those four layers in mind, the architecture becomes
much easier to follow.

---

## A Short Summary To Fall Asleep On

A request enters `cleanCrud` through a very small transport class.
It is translated into application language through adapters and facades.

From there, one of two main lanes takes over:

- CRUD, if the request is a normal create/fetch/update/delete action
- the operation lane, if the request is a typed creation or mutation command or
  event

In both cases, the lane-specific service builds a workflow.
That workflow is executed by one shared engine.

The engine gives the framework its rhythm:

- do the main business work in one transaction
- persist durable follow-up tasks if needed
- after commit, run lightweight post-commit hooks
- after commit, nudge durable follow-up execution

The aggregate definition acts as the contract that tells the framework what the
aggregate is and how it behaves.

When relationships are involved, coordinators do the heavy lifting.
When typed operation requests are involved, handlers and source-aware policy do
the heavy lifting.

The result is a framework that still looks like CRUD from the outside, but is
already much richer underneath:

- aggregate-aware
- workflow-owned
- source-aware
- operation-family aware
- post-commit capable
- durable-process capable
- creation-quarantine capable
- mutation-quarantine capable

That is the architecture as it exists today.
