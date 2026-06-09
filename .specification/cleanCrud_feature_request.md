# cleanCrud Feature Request

## Context

Building a trading engine on top of cleanCrud. Order submission and cancellation fit
the CRUD model well — domain model, insertion/deletion policies, persistence services,
REST controllers all wire up cleanly. Two gaps surfaced.

---

## Feature 1: Post-operation hooks

### What's needed

Optional hooks on `AggregateCrudDefinition` that fire **after** successful persistence:

```java

@FunctionalInterface
public interface PostSaveHook<ID, Model>
{
	void onSaved(IdentifiedModel<ID, Model> saved);
}

@FunctionalInterface
public interface PostDeleteHook<ID, Model>
{
	void onDeleted(ID id, Model modelBeforeDeletion);
}
```

Wired via the existing fluent builder:

```java
AggregateCrudDefinitions.aggregateCrudDefinition()
// ... existing fields ...
    .

postSaveHook(myPostSaveHook)          // optional
    .

postDeleteHook(myPostDeleteHook)      // optional
    .

build();
```

The engine invokes the hook after the persistence layer commits, before returning to
the caller. If the hook throws, the operation is considered failed (the caller receives
an error; whether to roll back depends on the engine's transaction boundary design).

### Our use case

After an Order is persisted as `SUBMITTED`, we must call an external broker API
(`placeOrder`) with the saved order details. After an Order is updated to `CANCELLED`,
we must call `cancelOrder` on the same API. Both are fire-and-confirm: the broker
acknowledges synchronously, then pushes status updates asynchronously.

The `InsertionPolicy` already handles pre-save validation (risk checks). The post-save
hook handles the outbound side-effect. Without it, the side-effect logic has to live
outside the framework's lifecycle, which breaks uniformity.

### General usefulness

Any system that needs to trigger side-effects after a successful write: send a webhook,
publish a domain event, call a downstream API, invalidate a cache, notify an audit log.
The hook is the standard "outbox-lite" pattern without requiring a full outbox.

---

## Feature 2: Singleton aggregate support

### What's needed

A variant of `AggregateCrudDefinition` for resources that always have exactly one
instance — no create, no delete, only read and update:

```java
AggregateCrudDefinitions.singletonAggregateCrudDefinition()
    .

fetchPort(fetchPort)
    .

mutationPort(mutationPort)        // update only
    .

patcher(patcher)
    .

responseBuilder(responseBuilder)
    .

patchPolicy(patchPolicy)          // validate updates
    .

build();
```

The REST surface would be:

```
GET  /resource          read the singleton
PUT  /resource          replace (full update)
PATCH /resource         partial update
```

No `POST /resource` (already exists) and no `DELETE /resource` (immutable existence).
The persistence layer enforces the single-row constraint; the framework exposes only the
read/write endpoints.

### Our use case

A `RiskConfiguration` entity holds system-wide trading controls: kill switch state,
maximum open orders, maximum position size per instrument, available funds buffer.
Operators read and update this configuration via the API; the `InsertionPolicy` for
orders reads it during pre-trade validation.

This is currently implemented as a hand-rolled persistence table (`kill_switch`) with
custom REST endpoints. It should be a first-class cleanCrud resource with full
`PatchPolicy`, `DomainSecurityPolicy`, and the standard controller base classes.

### General usefulness

Any application with a global settings or configuration resource: feature flags, rate
limits, circuit breaker thresholds, maintenance mode flags. The pattern is common and
currently requires stepping outside the framework entirely.

---

## Summary

| Feature                        | Existing hook       | Gap                                         |
|--------------------------------|---------------------|---------------------------------------------|
| Pre-save validation            | `InsertionPolicy` ✅ | —                                           |
| Pre-delete validation          | `DeletionPolicy` ✅  | —                                           |
| Post-save side-effect          | —                   | `PostSaveHook` ← needed                     |
| Post-delete side-effect        | —                   | `PostDeleteHook` ← needed                   |
| Singleton read/update resource | —                   | `singletonAggregateCrudDefinition` ← needed |

Both additions are small surface area, compositionally optional, and solve problems that
appear in essentially every non-trivial CRUD system. Happy to contribute implementations
or tests if useful.