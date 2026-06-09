package de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation;

import de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant.InvariantViolation;

import java.util.Objects;
import java.util.Optional;

public record MutationPolicyViolation(
		MutationViolationKind kind,
		String message,
		Optional<InvariantSeverity> severity)
{
	public static MutationPolicyViolation access(final String message)
	{
		return new MutationPolicyViolation(MutationViolationKind.ACCESS, message, Optional.empty());
	}

	public static MutationPolicyViolation transition(final String message)
	{
		return new MutationPolicyViolation(MutationViolationKind.TRANSITION, message, Optional.empty());
	}

	public static MutationPolicyViolation externalConsistency(final String message)
	{
		return new MutationPolicyViolation(MutationViolationKind.EXTERNAL_CONSISTENCY, message, Optional.empty());
	}

	public static MutationPolicyViolation invariant(final InvariantViolation violation)
	{
		return new MutationPolicyViolation(
				MutationViolationKind.INVARIANT,
				violation.message(),
				Optional.of(violation.severity()));
	}

	public MutationPolicyViolation
	{
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(message, "message");
		severity = Optional.ofNullable(severity).orElse(Optional.empty());
	}
}
