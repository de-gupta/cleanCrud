package de.gupta.clean.crud.template.useCases.incantation.domain.policy.violation;

import de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant.InvariantViolation;

import java.util.Objects;
import java.util.Optional;

public record IncantationPolicyViolation(
		IncantationViolationKind kind,
		String message,
		Optional<InvariantViolation> invariantViolation)
{
	public static IncantationPolicyViolation access(final String message)
	{
		return new IncantationPolicyViolation(IncantationViolationKind.ACCESS, message, Optional.empty());
	}

	public static IncantationPolicyViolation creation(final String message)
	{
		return new IncantationPolicyViolation(IncantationViolationKind.CREATION, message, Optional.empty());
	}

	public static IncantationPolicyViolation externalConsistency(final String message)
	{
		return new IncantationPolicyViolation(
				IncantationViolationKind.EXTERNAL_CONSISTENCY,
				message,
				Optional.empty());
	}

	public static IncantationPolicyViolation invariant(final InvariantViolation violation)
	{
		return new IncantationPolicyViolation(
				IncantationViolationKind.INVARIANT,
				violation.message(),
				Optional.of(violation));
	}

	public IncantationPolicyViolation
	{
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(message, "message");
		Objects.requireNonNull(invariantViolation, "invariantViolation");
	}
}
