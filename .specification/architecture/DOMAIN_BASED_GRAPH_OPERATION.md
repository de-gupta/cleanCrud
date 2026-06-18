# Domain-Based Graph Operation Blueprint

## Purpose

This document captures the target architecture for a new substrate-first framework shape.

The goal is to stop treating CRUD as the center of the system and instead make the domain substrate the primary product.
CRUD, operation lanes, CLI flows, and other transports become thin consumers of that substrate.

This document is intended as a blueprint for implementation work by a future agent.

## Core Idea

The architecture should be centered on two substrate concepts:

1. `Model`
2. `Graph`

Everything else builds on top of them.

- A `Model` is the atomic persisted domain node.
- A `Graph` is an operation boundary rooted at one model and spanning related models through explicit relationship
  definitions.

The framework should provide first-class services for both model-level and graph-level operations.

## Why This Exists

The current repository discovered many good concepts over time:

- model builders and patchers
- domain policies
- aggregate relationships
- lifecycle semantics
- reconciliation strategies
- mutation intents
- post-commit hooks
- quarantine and replay ideas
- tri-temporal history and auditing

However, these concepts were historically pulled around by CRUD and later operation-lane work. The result is useful
behavior but muddled architectural ownership.

The new direction should:

- make substrate concepts explicit first
- let lanes delegate downward into substrate services
- make transport concerns purely adaptive
- make security, history, and metadata lane-agnostic

## Substrate Layers

### 1. Model Substrate

The model substrate describes and operates on one domain node.

It should include:

- `ModelDefinition<Id, Model, Create, Patch>`
- create builder
- patcher
- fetch port
- mutation port
- domain policies
- security contract
- metadata capability declarations

It should provide services such as:

- `ModelCreateService`
- `ModelFetchService`
- `ModelUpdateService`
- `ModelDeleteService`

These are atomic services. They do not coordinate graph traversal.

### 2. Graph Substrate

The graph substrate describes and operates on a rooted domain graph.

It should include:

- `GraphDefinition<RootId, RootModel, RootCreate, RootPatch>`
- root `ModelDefinition`
- relationship definitions
- traversal semantics
- reconciliation strategy
- lifecycle semantics
- hydration rules
- intent language for graph mutation

It should provide services such as:

- `GraphCreateService`
- `GraphFetchService`
- `GraphUpdateService`
- `GraphDeleteService`

These services are relationship-aware and perform coordinated multi-node work.

## Model vs Graph

This distinction is essential.

A graph is not merely a model with optional satellites.

A model defines:

- one node
- its create/patch language
- its policy and persistence behavior

A graph defines:

- a root node
- relationships to child node definitions
- traversal and coordinated mutation behavior across nodes

This avoids the false idea that "everything is an aggregate all the way down".

## Nested Structure

A child node may itself have children.

This does not mean the framework should recursively invoke "aggregate services inside aggregate services".

Instead:

- recursion belongs to graph definition structure
- not to service boundary composition

The same domain type may appear in two roles:

- as a child node inside some parent graph
- as a root graph in its own right elsewhere

That is valid and expected.

The framework should therefore treat:

- `ModelDefinition` as node-level structure
- `GraphDefinition` as rooted operation boundary

## Relationship Language

Relationship definitions are one of the strongest concepts already proven in the current repo and should remain central.

Relationship metadata should include at least:

- relationship name
- child model/node definition
- cardinality
- reconciliation strategy
- persistence order
- link strategy
- identity resolver
- hydration strategy
- create input resolver
- patch input resolver
- lifecycle semantics

### Lifecycle Semantics

Lifecycle semantics should remain explicit relationship behavior:

- cascade create
- cascade update
- cascade delete
- orphan delete
- hydrate on fetch

These are graph-level concerns, not transport concerns.

### Reconciliation Strategy

The graph substrate should continue to support explicit reconciliation behavior, for example:

- `REPLACE`
- `MERGE_BY_ID`

No lane should reimplement reconciliation semantics.

## Intent Language

Graph mutation should use explicit intent types rather than hidden conventions.

For create flows, typical intents include:

- inline child create
- reference existing child

For update flows, typical intents include:

- create child
- reference child
- update child by id
- remove child by id
- current-child variants where cardinality semantics allow them

The intent language should be part of the substrate and reusable by all lanes.

## Policy Language

Policies should be domain-first and lane-agnostic.

Minimum policy categories:

- access policy
- insertion policy
- patch policy
- deletion policy

The new framework should strongly consider making rich policy decisions the default substrate language rather than
exceptions-only semantics.

That means policy outputs may eventually support:

- allow
- reject
- quarantine
- violations
- tolerated violations

Lanes can then decide how to present those outcomes, but the substrate remains the source of truth.

## Workflow and Runtime

The framework should keep the clarified distinction:

- `Workflow` = description of an operation flow
- `Runtime` = execution of that workflow

### Workflow

A workflow describes:

- what runs in transaction
- what runs after commit
- what durable follow-up work is emitted
- whether the flow is read-only, if supported later

### Runtime

A workflow runner executes that workflow:

- opens transactions
- runs transactional work
- dispatches after-commit logic
- triggers durable follow-up behavior

This belongs below lanes and is reused by model and graph services.

## Security

Security must be first-class and substrate-enforced.

It should not be optional per lane.

The substrate should define authorization/visibility rules at domain level, and all lanes must go through those rules.

This implies:

- security belongs in model and graph definitions/policies
- transport layers supply principal/context only
- transport layers cannot bypass enforcement

## Ownership Boundaries

The substrate should be the semantic center, but it must not become an undifferentiated monolith.

The key distinction is:

- substrate owns contracts and orchestration semantics
- infrastructure owns concrete implementations
- lanes own transport extraction and adaptation

### The Substrate Owns

The substrate should own:

- model and graph contracts
- operation semantics
- relationship semantics
- policy contracts
- security contracts
- metadata capability contracts
- quarantine contracts
- history/temporality contracts
- orchestration rules for when collaborators are invoked
- context models required for domain decisions

The substrate is responsible for saying:

- what information is required
- what decision points exist
- what capabilities are mandatory
- when persistence, policy, quarantine, history, or follow-up behavior must be invoked

### Infrastructure Owns

Infrastructure should own:

- concrete repository implementations
- JPA/Spring adapters
- concrete persistence stacks
- concrete history storage
- concrete quarantine storage
- process starter implementations
- security integration adapters
- concrete policy implementations that depend on deployment/runtime systems

Infrastructure should not redefine semantics.
It should implement substrate-defined ports.

### Lanes Own

Lanes should own:

- transport DTOs
- parsing and extraction of transport details
- mapping from transport requests into substrate context and inputs
- mapping from substrate results into transport responses

Lanes should not own:

- core policy meaning
- relationship logic
- security enforcement rules
- quarantine semantics
- history semantics

## Ports vs Implementations

The substrate should not directly own concrete repositories or persistence stacks.

Instead:

- substrate owns persistence ports
- infrastructure owns repository implementations/adapters

Examples of substrate-owned ports may include:

- model fetch/mutation ports
- graph fetch/mutation ports
- history recording ports
- quarantine persistence ports
- follow-up process ports
- clock/time ports if needed

This preserves substrate centrality without forcing infrastructure into the domain layer.

## Policy Ownership

Policies are not all the same. The architecture should distinguish at least three categories.

### Pure Domain Policies

These belong directly to substrate/domain logic.

Examples:

- invariants
- reconciliation constraints
- lifecycle restrictions
- domain-state-based access rules

These should be stable and definitional.

### Runtime-Injected Policies

These depend on environment or deployment concerns but still participate in substrate-defined decision points.

Examples:

- tenant-aware restrictions
- role/permission-backed rules
- source-aware restrictions
- feature-flagged policy variants

For these:

- substrate owns the policy contract
- substrate owns the policy input model
- substrate owns the orchestration point where the policy is evaluated
- infrastructure or application wiring may inject the concrete implementation

### Lane Presentation Policies

Some differences are only about presentation or result interpretation.

Examples:

- how `REJECT` is rendered in a REST response
- how a quarantine result is shown in a CLI

These belong above substrate, but only as result adaptation. The underlying decision semantics remain substrate-owned.

## Source-Aware Policies

If a policy needs to know the source of an operation, then source cannot remain a raw transport-only concern.

The correct pattern is:

- transport extracts source details
- lane maps them into substrate context
- substrate policies evaluate against that context

This implies a substrate-level operation context model such as:

- actor
- source/channel
- tenant
- correlation id
- causation id
- timestamps
- provenance metadata

The substrate must never depend on HTTP headers, CLI argument parsers, or message-broker types directly.
But it may absolutely depend on a substrate-level context model populated by those transports.

## Quarantine Ownership

Quarantine is not a transport concern.
It is a substrate/domain operation capability.

That means:

- substrate owns quarantine outcome semantics
- substrate owns quarantine record contracts
- substrate owns replay contracts
- infrastructure owns the concrete persistence adapter
- lanes may enrich the substrate context with provenance/source metadata used by quarantine logic

Quarantine should therefore be treated similarly to history/audit:

- semantically substrate-owned
- concretely adapter-backed

## Mandatory vs Declared Capabilities

The lane must not be allowed to opt out of a capability that a substrate definition requires.

This does not necessarily mean every model or graph in the entire framework must use every capability.

The better rule is:

- substrate definitions declare which capabilities apply
- once declared, all lanes must honor them

For example:

- a model/graph may be historized
- a model/graph may require quarantine support
- a model/graph may require security enforcement

The lane cannot disable those concerns.

## Context as the Boundary Object

Whenever a concern originates outside the substrate but affects domain decisions, it should be lifted into a substrate
context model rather than kept as a transport detail.

This applies especially to:

- actor identity
- source/channel
- tenant
- provenance
- correlation/causation
- timestamps

This is the key pattern that allows source-aware policies, security rules, auditing, and quarantine decisions to remain
substrate-driven without coupling the substrate to transport classes.

## Audit and Temporality

Audit and temporality should be substrate-level capabilities, not CRUD-owned concerns.

The current repository already contains valuable infrastructure for:

- audit actors
- tri-temporal history snapshots
- temporal change types
- temporal validity
- history recording

These capabilities should be detached from CRUD ownership and reintroduced as substrate features.

Important principle:

- lane cannot opt out of a capability once the substrate definition requires it

The substrate may allow capability declarations per model or graph, but transport lanes should not control whether those
capabilities are enforced.

## Administrative / Metadata Capabilities

Additional cross-cutting capabilities should sit beside security and temporality as substrate concerns.

Examples:

- provenance
- correlation / causation metadata
- source metadata
- quarantine references
- replay inputs
- audit actor capture
- compliance-oriented annotations

These capabilities should be part of substrate contracts, with infrastructure adapters handling persistence concerns.

## Lane Responsibilities

Lanes should be intentionally thin.

### What Lanes Should Own

- transport DTOs
- transport-specific request models
- adapters from transport input to substrate input
- adapters from substrate results to transport output
- lane-specific presentation semantics

### What Lanes Should Not Own

- graph reconciliation logic
- security enforcement
- policy semantics
- persistence orchestration
- relationship traversal
- audit/temporality decisions
- graph lifecycle semantics

CRUD, operation lane, CLI, messaging adapters, and web adapters should all follow this rule.

## CRUD Lane in the New Architecture

CRUD becomes just one consumer of the substrate.

CRUD should:

- adapt resource-oriented DTOs to model or graph inputs
- call substrate services
- map substrate outcomes into resource-oriented results/exceptions

CRUD should no longer be treated as the implicit owner of graph behavior.

## Operation Lane in the New Architecture

Operation lanes become another consumer of the same substrate.

Operation lane should:

- accept business payloads
- resolve handlers/adapters
- map payloads into substrate model/graph language
- interpret rich policy outcomes
- invoke quarantine/replay behavior where needed

Operation lane should not reimplement create/update graph semantics.

## CLI and Other Transports

A CLI lane should be treated the same way:

- parse command input
- adapt into substrate model/graph language
- invoke substrate services
- present results

The same applies to:

- REST
- GraphQL
- messaging consumers
- batch jobs
- internal service APIs

## Proposed High-Level Package Shape

This is illustrative, not final:

```text
domain/
  model/
    definition/
    policy/
    service/
    port/
    capability/
  graph/
    definition/
    relationship/
    intent/
    service/
    mechanics/
    policy/
  workflow/
  runtime/

infrastructure/
  persistence/
  history/
  security/
  process/
  quarantine/

lanes/
  crud/
  operation/
  cli/
  web/
```

Alternative naming is acceptable if the ownership remains equally clear.

## Recommended Implementation Principles

1. Design substrate APIs before implementing lanes.
2. Prefer class-level generics when a collaborator is conceptually typed.
3. Use method-level generics only for truly stateless utility behavior.
4. Avoid enum-based fake utility/factory classes.
5. Prefer explicit contracts over static service-locator style factories.
6. Keep policy, security, audit, and temporality lane-agnostic.
7. Let graph services own relationship-aware behavior from day one.
8. Treat the current repo as a source of proven behavior, not as the new foundation.

## Proven Concepts Worth Reusing from the Current Repository

These existing ideas are worth preserving:

- model builder / patcher separation
- policy categories
- relationship cardinality and reconciliation semantics
- lifecycle semantics
- explicit mutation intents
- post-commit mutation context
- workflow/runtime split
- quarantine and replay concepts
- tri-temporal history substrate

These should be selectively ported into the new foundation with cleaner ownership.

## Things to Avoid Repeating

- making CRUD the architectural center
- hiding substrate ownership inside CRUD packages
- mixing workflow/runtime concerns with graph mechanics
- allowing lanes to own domain semantics
- using package naming that suggests domain purity for service-layer mechanics
- letting transport layers decide whether security/history/audit apply

## Migration Strategy

If a new repo is created, the recommended approach is:

1. Define the substrate vocabulary first.
2. Implement model substrate.
3. Implement graph substrate.
4. Recreate workflow/runtime beneath both.
5. Reintroduce security and metadata capabilities.
6. Reintroduce history/audit/temporality as substrate capabilities.
7. Port proven graph behavior from the current repo.
8. Only then rebuild CRUD and operation lanes as thin adapters.
9. Fold sample implementation behavior into tests in the new repo.
10. Port generator behavior only after substrate contracts are stable.

## Suggested Initial Deliverables for a Future Agent

The first implementation pass should produce:

1. `ModelDefinition`
2. `GraphDefinition`
3. relationship definition language
4. intent language
5. workflow/runtime contracts
6. model create/fetch/update/delete services
7. graph create/fetch/update/delete services
8. one minimal thin CRUD adapter
9. one minimal thin operation adapter
10. tests covering nested graph behavior

## Final Architectural Rule

The substrate is the product.

Lanes are consumers of the substrate.

If a lane needs to do real domain work directly, the substrate is missing an abstraction and should be expanded instead
of allowing the lane to own that behavior.