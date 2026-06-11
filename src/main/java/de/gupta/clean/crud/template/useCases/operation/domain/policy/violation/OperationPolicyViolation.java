package de.gupta.clean.crud.template.useCases.operation.domain.policy.violation;

import de.gupta.clean.crud.template.useCases.operation.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.invariant.InvariantViolation;

import java.util.Objects;
import java.util.Optional;

public record OperationPolicyViolation(
		ViolationKind kind,
		String message,
		Optional<InvariantSeverity> severity)
{
	public static OperationPolicyViolation access(final String message)
	{
		return new OperationPolicyViolation(ViolationKind.ACCESS, message, Optional.empty());
	}

	public static OperationPolicyViolation core(final String message)
	{
		return new OperationPolicyViolation(ViolationKind.CORE, message, Optional.empty());
	}

	public static OperationPolicyViolation externalConsistency(final String message)
	{
		return new OperationPolicyViolation(ViolationKind.EXTERNAL_CONSISTENCY, message, Optional.empty());
	}

	public static OperationPolicyViolation invariant(final InvariantViolation violation)
	{
		return new OperationPolicyViolation(ViolationKind.INVARIANT, violation.message(),
				Optional.of(violation.severity()));
	}

	public OperationPolicyViolation
	{
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(message, "message");
		severity = Optional.ofNullable(severity).orElse(Optional.empty());
	}
}
