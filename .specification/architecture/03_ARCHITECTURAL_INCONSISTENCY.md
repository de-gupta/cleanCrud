# Architectural Inconsistency Sweep

## Why This Note Exists

`cleanCrud` has grown in layers.
The old single-entity CRUD path was explicit, class-shaped, and easy to follow:

- controller
- application controller
- facade
- service
- persistence

The newer aggregate CRUD and mutation-oriented path are stronger
architecturally, but they are no longer equally explicit at every seam.
This note records where the architecture is intentionally different, where it
is merely uneven, and where the documentation needs correction.

The goal is not to accuse the design of being wrong.
The goal is to separate:

- deliberate abstraction
- acceptable asymmetry
- accidental opacity

---

## Naming: What Should The "Second Lane" Be Called?

At the moment the least misleading name is still:

- **application mutation lane**

That is not beautiful, but it is accurate.

Why not "event source lane"?
Because the lane is broader than external events.
It accepts:

- user intent
- internal commands
- authoritative external events
- process-emitted actions
- administrative replay

So this is not really an event-only lane.
It is a lane for **typed state-changing application messages with source
semantics**.

If we want a better architectural name later, the strongest candidates are:

- **command/event mutation lane**
- **message-driven mutation lane**
- **application mutation lane**

For now, `application mutation lane` remains the safest term because it does
not over-claim.

---

## First Big Observation

There are now three eras visible in the codebase:

1. old explicit single-entity CRUD
2. aggregate CRUD
3. application mutation

They do not present the same shape to a reader.

That is the core inconsistency.
It does not necessarily mean the architecture is broken, but it does mean the
reader has to keep switching mental models.

---

## The Old Single-Entity CRUD Shape

The old shape was explicit and easy to reason about because every layer had a
named class and that class usually looked like the role it played.

Typical flow:

- small REST controller
- small application controller
- explicit facade
- explicit abstract framework service
- explicit persistence service

This style optimized for:

- readability
- obvious call flow
- low surprise

Its cost was repetition and a lot of wrapper classes.

---

## Aggregate CRUD: Stronger Semantics, Less Obvious Shape

Aggregate CRUD improved the architecture in important ways:

- workflows are owned by services, not the engine
- relationship semantics are centralized
- transaction and post-commit concerns are centralized
- durable subprocess support is shared

That is all real improvement.

But readability became less direct because a lot of behavior moved into
runtime helpers:

- `AggregateSaveCoordinator`
- `AggregateFetchCoordinator`
- `AggregateUpdateCoordinator`
- `AggregateDeleteCoordinator`
- `AggregateDefinitionGuard`
- `AggregateMutationValidationSupport`
- relationship planners / resolvers

These are not bad abstractions.
In fact, most of them are justified.
But they do make aggregate CRUD feel less class-shaped than the old path.

### Is That Deliberate?

Yes, largely.

The aggregate path is solving a different problem from the old single-entity
path.
Once relationships exist, some logic has to live below the use-case service:

- relationship planning
- hydration
- reconciliation
- orphan deletion
- cascade semantics

If that logic stayed directly inside the services, the services would become
massive and unreadable.

So the coordinators are not accidental "utilities".
They are the place where aggregate relationship semantics become executable.

### What Still Feels Uneven?

Two things.

First, the coordinators look partly like first-class domain runtime objects and
partly like utility helpers.
That is because they are instantiated through
`AggregateServiceSupportFactory` instead of being visible Spring beans or
aggregate-scoped collaborators.

Second, the old code trained the reader to expect a visible class per use-case
role.
Aggregate CRUD instead says:

- services own workflow shape
- helper runtime objects own relationship execution

That is more powerful, but less explicit on first read.

### Verdict

This inconsistency is **mostly deliberate and acceptable**.
The architecture is not muddled in principle.
It is simply less obvious than the old path because aggregate behavior really
is more complex.

---

## Application Mutation: More Powerful, More Hidden

The application mutation lane has the highest semantic power, but it is also
the lane where "hidden magic" is felt most strongly.

Why?

Because it combines several design moves at once:

- generic service factories
- generic application controller factories
- handler registry dispatch
- aggregate mutation planning
- source-aware policy evaluation
- quarantine integration
- durable subprocess integration

A lot of that is good architecture.
But the resulting object graph is not visually obvious in the same way that the
old explicit CRUD path was.

---

## Where The Current Documentation Was Wrong

The architecture document previously said that the mutation application
controller "packages the call as a `MutationRequest`".
That is only partly true.

The accurate statement is:

- the central `applyWithResult(MutationRequest<...>)` path **receives** an
  already-built `MutationRequest` and forwards it to the service
- the convenience default methods on `MutationApplicationController` can also
  **construct** a `MutationRequest` from `(id, payload, source, correlation,
  causation)` and then forward it

So the controller is not fundamentally a request-packaging layer.
It is fundamentally a thin forwarding layer with optional convenience overloads.

That distinction matters.

---

## Why There Is No Facade In The Mutation Lane

This is one of the most visible inconsistencies compared with old CRUD.

There is:

- no mutation facade
- no API/domain adapter pair in the main application mutation path
- no separate transport DTO translation layer in the core mutation flow

### Is That Intentional?

Yes.

The mutation lane was designed as an **application-first** lane, not a REST
lane.
Its main input is already a domain-level object:

- `MutationRequest`

and that request contains a typed application payload, not a transport DTO that
must be translated before business logic can begin.

So introducing a facade by default would often just create a forwarding wrapper
with little semantic value.

### Why This Makes Sense

The mutation lane is trying to model business intent more directly.
The key abstraction is:

- "apply this payload from this source to this aggregate"

That is already close to application language.
So the lane can reasonably go:

- controller
- service
- handler registry
- handler

without a dedicated facade in the middle.

### What Is The Cost?

The cost is asymmetry.
A reader coming from CRUD expects:

- controller
- facade
- service

but mutation instead exposes:

- controller
- service

That makes the architecture feel uneven even though it is conceptually
defensible.

### Verdict

This inconsistency is **deliberate and acceptable**.
The missing facade is not a design bug.
It is a consequence of the mutation lane starting from application-shaped
requests rather than transport-shaped DTOs.

---

## Why The Mutation Service Has Abstract Counterparts But The Controller Looks Factory-Made

There is an asymmetry here:

- `MutationService` exists
- `AbstractMutationService` exists
- `DefaultAggregateMutationService` exists
- `MutationApplicationController` exists
- `AbstractMutationApplicationController` exists
- but consumers usually do not write a visible concrete controller class

Instead they often get one via:

- `MutationApplicationControllers.controller(service)`

### Was This Deliberate?

Yes.

The mutation controller has very little consumer-specific behavior today.
It mainly forwards to the service.
So generating or hand-writing dozens of tiny concrete controller classes would
add explicitness, but not much meaning.

That is why the framework currently prefers:

- explicit concrete handler class
- generic controller instance produced by a factory

### Why It Still Feels Hidden

Because the resulting bean does not have the same narrative presence as a class
named, for example:

- `TaskMutationApplicationController`

The old path made object identity obvious through named classes.
The new path sometimes makes it obvious only through config wiring.

### Verdict

This is a **deliberate tradeoff**, but it is also the place where the mutation
lane most clearly feels more magical than the old CRUD path.

So the choice is defensible, but the feeling of opacity is real.

---

## Handler Registry Versus Direct Service Subclassing

The mutation lane routes behavior through:

- `MutationHandlerRegistry`
- `RegisteredMutationHandler`
- typed handler implementations

rather than expecting a separate service subclass per mutation.

### Is That A Good Choice?

Yes.

Once the lane is defined around typed payloads, registry-based dispatch is the
right abstraction.
Otherwise every new payload would pressure the framework toward either:

- giant `switch` statements
- or one service class per mutation payload

Both would be worse.

### Why Does It Feel More Magical Than CRUD?

Because CRUD routes by explicit method name:

- `save`
- `findById`
- `updateById`
- `deleteById`

Mutation routes by payload type discovered in a registry.

That is more open for extension, but less obvious on first read.

### Verdict

This is **deliberate and correct**, even if it is less explicit.

---

## The Real Inconsistency: What Is A Framework Object And What Is An Application Object?

This is the deepest source of confusion.

Across the repo, objects come from several different mechanisms:

- component-scanned concrete classes
- handwritten `@Bean` methods
- framework factory methods returning default implementations
- framework auto-configuration
- framework-internal non-Spring support factories

All of these are reasonable in isolation.
But together they make it less obvious which category a given class belongs to.

For example:

- a mutation handler is clearly an application object
- a facade is clearly an application object
- `DefaultAggregateMutationService` feels like framework runtime
- `AggregateSaveCoordinator` feels like framework runtime but is not a Spring
  bean
- `MutationApplicationController` is framework-facing API but often materialized
  by a factory in app config

So the architecture is not inconsistent in behavior so much as in
**object-creation visibility**.

### Verdict

This is the main readability issue.
It is not fatal, but it is real.

---

## Deliberate Choices That Should Stay Deliberate

The following asymmetries make sense and should not be "fixed" just for visual
symmetry:

### 1. No default facade in the application mutation lane

Reason:
the lane already starts from application-shaped input.

### 2. Registry-based mutation dispatch

Reason:
typed payload dispatch is the core extension model.

### 3. Coordinators below aggregate CRUD services

Reason:
relationship semantics need a lower execution layer.

### 4. Shared workflow engine across CRUD and mutation

Reason:
transaction, post-commit, durable-process start, and quarantine concerns belong
in one lifecycle executor.

### 5. Factory-created generic services and controllers

Reason:
for thin generic framework objects, a `@Bean` assembly method is clearer than a
custom subclass.

---

## Uneven Spots That Are Worth Acknowledging Openly

These are not necessarily urgent code changes, but they are genuine
inconsistencies in how the architecture presents itself.

### 1. Aggregate CRUD coordinators can read like utilities rather than first-class runtime collaborators

This is partly naming and partly visibility.
They do important work, but their construction is hidden in
`AggregateServiceSupportFactory`.

### 2. Mutation controller beans are less narratively visible than old CRUD controllers

A factory-returned controller is economical, but less legible than a named
concrete class.

### 3. The reader has to learn too many bean-construction modes

The framework now uses several creation styles, all valid, but not uniformly
discoverable.

### 4. The mutation lane feels more "inside the framework" than the CRUD lane

That is because mutation uses:

- handler registries
- policy engines
- quarantine machinery
- durable subprocess hooks

so the reader crosses into framework machinery earlier and more often.

---

## Short Assessment

The current architecture is not inconsistent in the sense of "internally
contradictory".
It is inconsistent in the sense of **presentation style**.

The old CRUD path optimized for explicit class-by-class legibility.
The aggregate CRUD path optimized for relationship-aware runtime composition.
The mutation path optimized for expressive message-driven behavior.

Those are different optimizations, so the code no longer looks uniformly
shaped.

That is mostly acceptable.
But the price is that newer lanes feel more magical unless the documentation is
very explicit about:

- what gets instantiated where
- which objects are framework runtime helpers
- which objects are application behavior
- which asymmetries are deliberate

---

## Bottom Line

The main conclusions are:

1. aggregate CRUD is more abstract than old CRUD, but for good reasons
2. the mutation lane is intentionally application-first and therefore does not
   need a facade by default
3. the mutation controller is thinner than previously described; its core role
   is forwarding, with convenience overloads that can construct a
   `MutationRequest`
4. the largest real inconsistency is not business semantics but object-creation
   visibility
5. most asymmetries are deliberate; they just need to be named so they do not
   feel accidental
