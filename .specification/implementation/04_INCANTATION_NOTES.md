# Incantation Notes And Deferred Design Seams

## Purpose

This file captures architectural observations that became clear while
hand-writing the first real incantation consumer in the sample
implementation.

The incantation lane is now credible enough to use, but the first slice also
exposed design seams that should be revisited once the broader operation model
settles.

---

## Current Shape Is Good Enough For Early Use

The current incantation implementation already proves a few important things:

- application-driven create operations can flow through a framework-owned lane
  that is not REST CRUD
- source-aware policy evaluation works
- post-commit hooks and durable subprocesses can still compose with that lane
- the public API is understandable enough for a handwritten consumer module

That is meaningful progress. The lane is not theoretical anymore.

---

## Design Seam: Create Semantics Currently Lean On Incantation Policy

While implementing the first consumer, one architectural seam became obvious:

- classic CRUD save centralizes several create concerns inside the save lane
- incantation create currently relies more heavily on incantation-specific
  creation policy and source-aware evaluation

In practice, that means incantation create is currently shouldering concerns
that the CRUD save lane historically centralized, especially:

- source-aware access semantics
- insertion semantics
- duplicate-related admission semantics

This is acceptable for the first slice, because the incantation lane is meant
to have richer semantics than plain REST create.

But it is still a real design seam, because the boundary between:

- generic aggregate create mechanics
- source-aware incantation policy
- classic CRUD validation support

is not yet fully settled.

---

## Why This Is Not A Bug

This should not be treated as an accidental defect in the current code.

The incantation lane was intentionally introduced as a parallel application
lane, not as a thin alias over CRUD save. That means some semantic divergence
is expected and even desirable.

The seam is therefore architectural, not merely local:

- some create concerns probably belong to a shared aggregate create substrate
- some concerns really are source-aware and therefore belong in incantation
  policy

The framework is not wrong today. It is simply at an intermediate point where
the final boundary has not yet been fully extracted.

---

## What To Revisit Later

Once the broader operation model is clearer, revisit these questions:

1. Which create validations should remain lane-specific?
2. Which create validations should be promoted into a shared aggregate-create
   substrate used by CRUD and incantation alike?
3. Should duplicate admission be expressed as a first-class incantation policy
   concept rather than indirectly through existing aggregate behavior?
4. Should incantation create return richer information about which shared
   versus source-aware policies participated?
5. How should this extend once incantation supports the same full aggregate
   scope as CRUD create?

---

## Working Decision

For now, keep the current direction.

The incantation lane is valuable and coherent enough to keep moving, but this
seam should be treated as:

- known
- deliberate
- worth revisiting before declaring the lane fully mature

Do not over-correct prematurely by collapsing incantation back into plain CRUD
save mechanics.
