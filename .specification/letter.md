Subject: Framework extension proposal — polymorphic update commands and event-driven mutations

Hi,

We're building a trading engine on top of cleanCrud and have hit a structural gap we'd like to discuss. The gap is real
enough that we think it's worth extending the framework rather than working around it.

**The use case**

Our Order aggregate is created and queried via REST in the standard cleanCrud way. The problem is on the update side.
Orders are mutated by two distinct sources:

- *HTTP-initiated commands* (cancel order) — user intent, should go through `PatchPolicy`
- *Broker-initiated events* (fill received, order acknowledged, order rejected) — authoritative facts from an external
  system, no validation needed, but should still go through the same persistence and audit path

These two sources have structurally different data payloads. A fill carries `(fillPrice, fillQty, filledAt)`. An
acknowledgment carries nothing. A rejection carries `(reason, rejectedAt)`. A cancellation carries `(cancelledAt)`. None
of these share a common patch shape.

Currently cleanCrud models a single `DomainModelUpdatePatch` type per aggregate and routes all updates through
`PatchPolicy`. That works for the HTTP-command case but breaks for the event-driven case in two ways:

1. To express four structurally different updates you're forced into a fat optional-field union DTO — exactly what a
   sealed type hierarchy exists to prevent
2. `PatchPolicy` semantics are wrong for broker events: a fill is a fact to be applied, not a request to be validated

**The two abstractions we'd like to see**

**1. Polymorphic update commands**

A sealed command hierarchy per aggregate, with the framework routing to the right handler via pattern matching:

```java
sealed interface OrderUpdateCommand
		permits FillCommand, AcknowledgeCommand, RejectCommand, CancelCommand
{
}

record FillCommand(Price fillPrice, Quantity fillQty, Instant filledAt)
		implements OrderUpdateCommand
{
}
```

The framework would expose a `CommandHandler<DomainModel, Command>` interface (applies a specific command type to the
domain model and returns the updated model) and a `UpdateCommandService` that dispatches to the right handler.

**2. Event-driven update pathway**

A way to apply update commands from within the application — not originating from an HTTP request — that:

- Bypasses `PatchPolicy` (or accepts a separate `EventPolicy` with different semantics: the event is authoritative, you
  may choose to reject it on invariant grounds but not on permission grounds)
- Uses the same persistence, audit trail, and `PostCommitMutation` machinery as HTTP-initiated updates
- Can be called from an async event consumer (in our case, an IBKR callback handler running on a background thread)

In practice, the IBKR callback handler would become a thin adapter that translates broker-specific types into
`OrderUpdateCommand` instances and passes them to this service — the same domain logic, the same persistence path, just
a different entry point.

**Why this matters beyond trading**

Any system with both an HTTP interface and an integration with an authoritative external system hits this pattern:
payment webhooks updating order state, logistics events updating shipment state, IoT telemetry updating device state.
The CRUD scaffolding is right for the HTTP side; event-driven mutation is the missing half.

We're building Metis with a bespoke event consumer in the meantime, but we'd rather align with cleanCrud long-term if
these abstractions land. Happy to collaborate on the design or contribute an implementation if useful.