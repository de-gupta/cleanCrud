# cleanCrud Feature Request: Singleton

## Context

Building a trading engine on top of cleanCrud. Order submission and cancellation fit
the CRUD model well — domain model, insertion/deletion policies, persistence services,
REST controllers all wire up cleanly. Two gaps surfaced.

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