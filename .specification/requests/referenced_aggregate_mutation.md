Subject: Mutation lane — referenced aggregate update from a handler

Hi,

Following on from the mutation lane architecture, we have a concrete use case
that sits at an edge of the current design. We want to lay out the problem and
the options honestly rather than jump straight to a feature request.

---

## The use case

In our trading system, when a broker fill arrives, two things must happen
atomically:

1. The **Order** aggregate transitions state: status → FILLED, fill price, fill
   quantity, and fill timestamp are recorded.

2. The **Position** aggregate for that instrument is updated: quantity increases,
   average cost is recalculated, unrealised P&L is updated.

The fill is an `AUTHORITATIVE_EXTERNAL_EVENT`. It is a single business fact from
the broker. It is not meaningful to say "the order was filled" without also
saying "the position grew". They are two consequences of one atomic event.

Position is a referenced independent aggregate, not an owned satellite of Order.
One position for a given instrument accumulates fills from many different orders
over time. Its lifecycle is independent of any single order. Modelling it as a
satellite would be semantically wrong.

So the handler for `ApplyFillMutation` on the Order aggregate needs to:

- update the Order root
- and also update the referenced Position aggregate

in a single transaction.

---

## The options as we see them

### Option A — handler calls the referenced aggregate's mutation controller directly

The `ApplyFillMutationHandler` injects `PositionMutationApplicationController`
and calls `applyInternalCommand(positionId, new UpdatePositionOnFillMutation(...))`
from within the handler body.

Since the handler executes inside the mutation workflow, which runs inside
`inTransaction()`, Spring's default REQUIRED propagation should cause the
position mutation to join the outer transaction. Both updates would be atomic.

**Pros:**

- Works today with no framework changes
- Explicit and readable orchestration
- Causation chain via correlation and causation IDs (position mutation can
  reference the fill mutation as its cause)
- Source semantics are correct: `INTERNAL_COMMAND` for the position update

**Cons:**

- Atomicity depends on REQUIRED propagation holding end-to-end. If the inner
  mutation service opens a new transaction (REQUIRES_NEW, or if the durable
  process start inside the inner call creates a boundary), the guarantee breaks.
- Policy evaluation runs twice — once for the order mutation, once for the
  position mutation — even though these are mechanically one event.
- Orchestration burden falls on the handler author. For every causally linked
  cross-aggregate mutation, they must know to inject and call the second
  controller.

**Conditions where this is sufficient:**
REQUIRED propagation is confirmed to hold throughout the inner mutation call,
including any durable process registration that the inner call might trigger.

---

### Option B — framework adds a `LinkedMutationIntent` in the `AggregateMutationPlan`

The handler returns an `AggregateMutationPlan` that includes not just the updated
root and owned satellite intents, but also one or more `LinkedMutationIntent`
entries:

```java
return AggregateMutationPlan.of(updatedOrder)
    .

withLinkedMutation(positionId, new UpdatePositionOnFillMutation(...),

MutationSource.INTERNAL_COMMAND);
```

The framework executes all linked mutations in the same transaction as the
primary mutation.

**Pros:**

- Atomicity is a framework guarantee, not a propagation assumption
- Handler API is clean — the handler expresses business intent, not orchestration
- Policy for the linked mutation is evaluated against the correct aggregate's
  definition with the correct source
- Causation wiring can be automatic — the framework sets causation ID on linked
  mutations

**Cons:**

- Non-trivial framework addition: the engine must coordinate multiple aggregate
  mutations in one transaction
- Raises policy composition questions: if the primary mutation is allowed but the
  linked mutation is quarantined, what is the overall outcome?
- Risk of misuse: developers may reach for linked mutations when they should be
  modelling the relationship differently
- The current clean separation between "mutation lane" and "referenced aggregate
  is independent" weakens slightly

**Conditions where this makes sense:**
The two mutations are genuinely one atomic business fact. Fill → position update
is the canonical example. Payment confirmed → ledger entry is another. These are
not arbitrary cross-aggregate side effects; they are two views of one event.

---

### Option C — post-commit hook triggers position mutation

The fill mutation's post-commit hook fires after the order transaction commits
and calls the position update.

**Pros:**

- Simple to wire
- No framework changes

**Cons:**

- Not atomic. The order is FILLED in the database before the position is updated.
  If the process dies between the two, the position is permanently stale.
- Post-commit is explicitly designed as lightweight best-effort. Financial
  position data is not a best-effort concern.

**Conditions where this is acceptable:**
Never for financial position state. Acceptable only for non-critical derived
views where eventual consistency is explicitly designed for.

---

### Option D — durable process from fill → position update

After the fill mutation commits, a durable process is started that updates the
position.

**Pros:**

- Retryable and recoverable if the process execution fails
- Durable process state is persisted

**Cons:**

- Not atomic with the order update. There is always a window, however brief,
  where the order is FILLED but the position has not yet been updated.
- Adds operational complexity: the position update can be observed in a
  "not yet applied" state.
- For a trading system, position state directly affects risk checks on subsequent
  orders. A stale position during that window means risk can be evaluated
  incorrectly.

**Conditions where this is acceptable:**
Where a brief inconsistency window is tolerable and recoverability matters more
than strict atomicity. Not suitable for position state in a trading system.

---

## What we are actually asking

Two things.

**First**, we would appreciate clarification on Option A: does calling a second
aggregate's mutation service from within a handler — in the context of the outer
mutation workflow's transaction — give us a guaranteed atomic boundary? We want
to understand whether the REQUIRED propagation holds throughout, including past
any internal durable process registration in the inner call. If yes, Option A is
sufficient for our current needs and we do not need any framework changes.

**Second**, if Option A does not give atomicity in all cases, we would like to
start a design conversation around Option B. We are not asking for this
immediately — we want to understand the transactional semantics first. But we
think the `LinkedMutationIntent` concept is narrow enough to design carefully and
broadly useful enough to justify. The fill → position case is not exotic; payment
→ ledger, shipment event → delivery state, IoT event → device aggregate are all
the same shape.

We are happy to contribute to the design or implementation of Option B if the
team agrees it is the right direction.

---

Thanks for the work on the mutation lane. The architecture as described in
`00_EXISTING_ARCHITECTURE.md` is exactly what we needed.