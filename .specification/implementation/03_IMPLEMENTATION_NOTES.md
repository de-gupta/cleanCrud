# Implementation Notes And Deferred Items

## Purpose

This file captures implementation-adjacent decisions that are important enough
to preserve, but not yet strong enough to justify immediate framework work.

It is intentionally lighter than the architecture and implementation roadmap
documents. The goal is to avoid re-litigating already-understood tradeoffs
while still keeping scope under control.

---

## Deferred: Quarantine Persistence And Replay

### Current state

The mutation subsystem now supports a rich quarantine outcome:

- source-aware mutation policies can return a quarantined decision
- mutation services return a `MutationResult` that exposes quarantine state
- callers can inspect violations and quarantine metadata
- the legacy convenience path still throws for callers that want exception
  semantics

This is enough for application-level semantics and testing, but it is not yet a
full operational quarantine subsystem.

### What is missing

The framework does **not** currently persist quarantined mutations as durable
review artifacts.

It also does not yet provide:

- a quarantine repository/model
- replay semantics
- reject/supersede semantics
- review/audit history for quarantined items
- operational APIs or endpoints for quarantine inspection

In the current implementation, quarantine is therefore:

- modeled
- returned to callers
- logged

but not yet durably managed.

### Why this is deferred

This is a meaningful feature in its own right, not a small follow-up.

A proper quarantine subsystem would need explicit answers for:

- what exactly is persisted
- what replay means
- whether replay uses the original source/profile or an override
- how replay history is tracked
- whether quarantine is manual-only or can be policy-driven later
- how operators inspect and disposition quarantined mutations

That is closer in scope to a sibling of durable processes than to a simple
adapter enhancement.

The trading-team request that triggered the broader mutation work did not
require this capability immediately. They needed richer mutation semantics and
source-aware handling, not an operational quarantine workbench.

### Working decision

Quarantine persistence and replay are explicitly deferred for now.

Treat this as a known limitation of the current mutation feature:

- quarantine is available as a semantic outcome
- quarantine is not yet a persisted operational queue

### Recommended future direction

When revisited, quarantine should be designed as its own feature with:

- persisted `QuarantinedMutation` records
- explicit lifecycle statuses
- application APIs for list/find/replay/reject
- later, optional REST/operator exposure

It should remain distinct from durable processes:

- durable process = work to execute or retry
- quarantine = work intentionally stopped pending review or resolution

### Constraint for future work

Do not try to smuggle partial quarantine persistence into unrelated mutation or
adapter changes.

If this feature is taken up later, it should be designed deliberately and
implemented as a coherent subsystem.
