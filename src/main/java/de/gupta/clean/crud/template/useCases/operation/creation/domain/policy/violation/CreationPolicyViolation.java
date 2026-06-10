package de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.violation;

import de.gupta.clean.crud.template.useCases.operation.domain.policy.invariant.InvariantViolation;

import java.util.Objects;
import java.util.Optional;

public record CreationPolicyViolation(
		CreationViolationKind kind,
		String message,
		Optional<InvariantViolation> invariantViolation)
{
	public static CreationPolicyViolation access(final String message)
	{
		return new CreationPolicyViolation(CreationViolationKind.ACCESS, message, Optional.empty());
	}

	public static CreationPolicyViolation creation(final String message)
	{
		return new CreationPolicyViolation(CreationViolationKind.CREATION, message, Optional.empty());
	}

	public static CreationPolicyViolation externalConsistency(final String message)
	{
		return new CreationPolicyViolation(
				CreationViolationKind.EXTERNAL_CONSISTENCY,
				message,
				Optional.empty());
	}

	public static CreationPolicyViolation invariant(final InvariantViolation violation)
	{
		return new CreationPolicyViolation(
				CreationViolationKind.INVARIANT,
				violation.message(),
				Optional.of(violation));
	}

	public CreationPolicyViolation
	{
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(message, "message");
		Objects.requireNonNull(invariantViolation, "invariantViolation");
	}
}
