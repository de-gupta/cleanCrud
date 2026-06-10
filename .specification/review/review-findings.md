# Code Review — Since v0.10.0

**Release tag**: v0.10.0 (2026-06-09)  
**Review date**: 2026-06-10  
**Commits since tag**: 18  
**Java files changed**: ~150 (new operation/creation and operation/mutation lanes, quarantine infrastructure, and codec
support)

---

## 1. Architecture & DDD

**Finding 1.1**

- **Location**: `DefaultAggregateMutationService.java` — constructor body
- **Issue**: `new` used directly for `SatelliteRelationshipPlanner`, `SatelliteReferenceResolver`, and
  `AggregateMutationValidationSupport` inside the constructor. Rule: no `new` for infrastructure objects — inject via
  constructor or assemble via factory.
- **Fix**: Obtain these collaborators through `AggregateServiceSupportFactory` (matching the creation-side pattern where
  `AggregateSaveCoordinator` is assembled there) and inject them:

```java
public DefaultAggregateMutationService(
    ...,
    final AggregateMutationCoordinator mutationCoordinator)
{
	this.mutationCoordinator = mutationCoordinator;
}
```

**Finding 1.2**

- **Location**: `JpaCreationQuarantineStore.java` line 27; `JpaMutationQuarantineStore.java` line 26
- **Issue**: Both classes expose a `public` constructor alongside a `public static with(...)` factory, and neither is
  declared `final`. When a static factory exists the constructor must be `private` and the class `final`.
- **Fix**:

```java
public final class JpaCreationQuarantineStore implements CreationQuarantineRepository
{
    ...

	private JpaCreationQuarantineStore(final EntityManager entityManager, final ObjectMapper objectMapper)
	{ ...}
}
```

**Finding 1.3**

- **Location**: `QuarantinedCreationException.java` line 3
- **Issue**: `QuarantinedCreationException` extends `RuntimeException` while the symmetric
  `QuarantinedMutationException` extends `DomainException`. Both sides of the domain should extend the same base type.
- **Fix**: `public final class QuarantinedCreationException extends DomainException`

**Finding 1.4**

- **Location**: `CreationPolicyViolation.java` line 11
- **Issue**: `CreationPolicyViolation` imports and uses `InvariantViolation` from the **mutation** domain package (
  `mutation.domain.policy.invariant.InvariantViolation`), creating an upward cross-domain dependency. The creation
  domain must not depend on a type owned by the mutation domain.
- **Fix**: Move `InvariantViolation` and `InvariantSeverity` to a shared `operation.domain.policy.invariant` package,
  then have both `CreationPolicyViolation` and `MutationPolicyViolation` import from there.

---

## 2. Code Style

**Finding 2.1**

- **Location**: `DefaultCreationQuarantineService.java` line 101; `DefaultMutationQuarantineService.java` line 109
- **Issue**: Exception catch parameter named `e` — abbreviation forbidden. Rule: always `caught`.
- **Fix**: `catch (RuntimeException caught)`

**Finding 2.2**

- **Location**: `DefaultCreationQuarantineService.java` line 138; `DefaultMutationQuarantineService.java` line 146
- **Issue**: Private method parameter named `e` in `summaryFor(final RuntimeException e)`.
- **Fix**: `private String summaryFor(final RuntimeException caught)`

**Finding 2.3**

- **Location**: `JacksonCreationQuarantinePayloadCodec.java` line 33; `JacksonMutationQuarantineValueCodec.java` line 32
- **Issue**: Catch parameters named `e` in all four catch blocks (two per file).
- **Fix**: Rename all to `caught`.

**Finding 2.4**

- **Location**: `JpaCreationQuarantineStore.java` lines 152, 163; `JpaMutationQuarantineStore.java` lines 153, 164
- **Issue**: Catch parameters named `e` in `serializeViolations` and `deserializeViolations`.
- **Fix**: Rename all to `caught`.

**Finding 2.5**

- **Location**: `AggregateCreationPolicies.java` line 121; `AggregateMutationPolicies.java` line 113
- **Issue**: `validate` override on `EvaluatingSourceAwareCreationPolicy` / `EvaluatingSourceAwareMutationPolicy` is
  declared `public`. Sealed interface implementations must be package-private (or lower). Even though the enclosing
  class is `private`, the `public` keyword is inconsistent with the stated rule.
- **Fix**: Remove `public` from both `validate` override declarations.

**Finding 2.6**

- **Location**: `CreationQuarantineInfrastructureAutoConfiguration.java` lines 50, 64, 78–79, 93, 114;
  `MutationQuarantineInfrastructureAutoConfiguration.java` (same pattern)
- **Issue**: `java.util.Optional`, `java.util.Collections`, and `java.util.Set` are written as fully-qualified inline
  names inside method bodies instead of being imported at the top. All other types in these files are imported.
- **Fix**: Add imports; remove inline qualifications.

**Finding 2.7**

- **Location**: `AggregateMutationCoordinator.java` line 109
- **Issue**: `java.util.stream.Collectors.toSet()` used as a fully-qualified call inline instead of via an import.
- **Fix**: Add `import java.util.stream.Collectors;` and use `Collectors.toSet()`.

---

## 3. Modern Java

Good use of modern java.

## 4. No Comments or Docs

No Javadoc or inline comments were found in the reviewed production files. This dimension is clean.

---

## 5. Testing

**Finding 5.1**

- **Location**: `DefaultCreationQuarantineServiceTest.java` line 29; `DefaultMutationQuarantineServiceTest.java` lines
  30, 50, 67, 84
- **Issue**: `@Test` methods are placed directly in the outer class, bypassing the required
  `outer → nested context → method` structure. `DefaultMutationQuarantineServiceTest` also uses the fully-qualified
  `@org.junit.jupiter.api.Test` annotation.
- **Fix**: Introduce `@Nested` context classes (e.g. `WhenRecordingSubmission`, `WhenReplaying`, `WhenDismissing`) and
  move all `@Test` methods into them. Use the static import for `@Test`.

**Finding 5.2**

- **Location**: `DefaultCreationQuarantineServiceTest.java` line 50; `DefaultMutationQuarantineServiceTest.java` line
  47; all assertions in both quarantine service tests, all contract tests, all controller tests
- **Issue**: No `assertThat(...)` call has a `.as(pattern, args...)` message. Rule: every assertion must have
  `.as(...)`.
- **Fix**:

```java
assertThat(persisted.quarantineId())
		.

as("quarantine record should carry a persisted id after recording")
    .

isPresent();
```

Apply to every assertion in every test file in the change set.

**Finding 5.3**

- **Location**: `DefaultCreationQuarantineServiceTest.java` line 99; `DefaultMutationQuarantineServiceTest.java` line
  96; `CreationContractsTest.java` lines 105–109; `MutationContractsTest.java` lines 88–102
- **Issue**: `assertThrows(RuntimeException.class, ...)` is JUnit API. Rule: AssertJ only.
- **Fix**:

```java
assertThatThrownBy(() ->service.

dismiss(dismissed.quarantineId()))
		.

as("dismissing an already-dismissed record should throw")
    .

isInstanceOf(RuntimeException .class);
```

**Finding 5.4**

- **Location**: `CreationContractsTest.java` and `MutationContractsTest.java` — multiple lines;
  `CreationPolicyContractsTest.java`; `MutationPolicyContractsTest.java`
- **Issue**: `assertEquals(...)`, `assertFalse(...)`, `assertTrue(...)` (JUnit assertions) used throughout. Rule:
  AssertJ only.
- **Fix**: Replace all with AssertJ equivalents:

```java
// assertEquals(expected, actual)  →
assertThat(actual).

as("...").

isEqualTo(expected);

// assertTrue(condition)  →
assertThat(actual).

as("...").

satisfies(v ->

assertThat(v).

isTrue());
// or: assertThat(actual).as("...").isTrue()  when asserting on a plain boolean, not a domain accessor
```

**Finding 5.5**

- **Location**: `DefaultMutationQuarantineServiceTest.java` line 122
- **Issue**: `public record AcknowledgeOrder(...)` declared inside the test class. Test helpers must be `private`.
- **Fix**: `private record AcknowledgeOrder(String value) implements ApplicationOperationPayload`

**Finding 5.6**

- **Location**: `AggregateMutationServicesTest.java` line 647; `AggregateCreationServicesTest.java` —
  `QuarantiningAggregateDefinition`
- **Issue**: Concrete inner helper classes used only as leaf types are not declared `final`. Rule: every concrete class
  that is not designed for extension is `final`.
- **Fix**: Add `final` to `QuarantiningAggregateDefinition` and any other concrete helper class that is not itself a
  base for further extension within the test.

**Finding 5.7**

- **Location**: `JpaCreationQuarantineStoreTest.java`; `JpaMutationQuarantineStoreTest.java` — all assertions
- **Issue**: No `.as(...)` on any assertion (same rule as Finding 5.2).
- **Fix**: Add `.as(...)` to every `assertThat(...)` call.

**Finding 5.8**

- **Location**: `MutationQuarantineApplicationControllerTest.java` — all `@Test` methods in outer class
- **Issue**: Same flat structure violation as Finding 5.1 — `@Test` methods not nested inside a context class.
- **Fix**: Introduce `@Nested` context classes.

---

**Several issues found across 3 dimensions**