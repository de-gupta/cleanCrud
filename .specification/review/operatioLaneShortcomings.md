# Operation Lane Shortcomings

This note captures additional operation-lane debt observed during the recent
creation quarantine persistence and replay implementation work.

It is intentionally focused on shortcomings that were still fresh from the
implementation itself, not on older review or refactoring notes.

## Current Debt

- Quarantine persistence is still stringly typed. We persist `payloadType` plus
  `payloadJson`, and for mutation also `domainIdType` plus `domainIdJson`. This
  works, but replay is coupled to runtime class names and Jackson shape
  stability.

- Replay identity is bean-name driven. Using Spring bean names as
  `aggregateType` is practical, but it is not a strong semantic contract. A
  rename can silently break replay of older quarantine records.

- Replay discovery is too magical. The lazy bean-factory lookup plus proxy
  unwrapping is robust enough, but it is subtle framework behavior and not a
  clean explicit registration model.

- Creation and mutation quarantine remain largely parallel-copy in structure. We
  deliberately avoided forcing a premature generic abstraction, but there is
  clearly shared lifecycle machinery that is not yet named or extracted well.

- The codec seam fixed dependency direction, but the serialization contract is
  still under-modeled. Layering is better, but there is no clear versioned
  payload-envelope concept yet.

- Quarantine records expose persistence-oriented internals upward. API and web
  responses still surface raw `payloadType` and JSON blobs, which mixes admin
  diagnostics with stored replay mechanics.

- Replay outcome modeling is weak. Values such as `APPLIED`, `FAILED`, and
  `QUARANTINED`, plus free-form summary strings, are sufficient for now but not
  a strong domain model if replay semantics grow.

- There is no clear concurrency story yet. The current implementation does not
  establish a strong guard against multiple concurrent replay attempts on the
  same open quarantine.

- There is no migration or versioning story for old quarantines. If payload
  classes move, are renamed, or change shape, replay becomes fragile.

- The operation lane still lacks a crisp public shape. Internal capability has
  grown faster than the conceptual surface has been cleaned up, which is a bad
  point to freeze generator behavior against.

- Testing is still targeted rather than systemic. The feature works and the main
  paths are covered, but the lane is not yet hardened for broad safe evolution.

## Highest-Value Cleanup Order

1. Replace bean-name aggregate identity with an explicit stable replay key.
2. Clarify replay record format and payload versioning contract.
3. Reduce quarantine duplication where lifecycle behavior is actually shared.
4. Simplify replay-gateway registration so it is explicit rather than
   container-clever.
