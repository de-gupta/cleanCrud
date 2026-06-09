package de.gupta.clean.crud.template.useCases.mutation.domain.policy.evaluation;

import de.gupta.clean.crud.template.useCases.mutation.domain.policy.quarantine.MutationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation.MutationPolicyViolation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MutationPolicyDecision(
		boolean allowed,
		List<MutationPolicyViolation> toleratedViolations,
		Optional<MutationQuarantineRequest> quarantineRequest)
{
	public static MutationPolicyDecision allow()
	{
		return allow(List.of());
	}

	public static MutationPolicyDecision allow(final List<MutationPolicyViolation> toleratedViolations)
	{
		return new MutationPolicyDecision(true, toleratedViolations, Optional.empty());
	}

	public static MutationPolicyDecision quarantine(final MutationQuarantineRequest quarantineRequest)
	{
		return quarantine(quarantineRequest, List.of());
	}

	public static MutationPolicyDecision quarantine(
			final MutationQuarantineRequest quarantineRequest,
			final List<MutationPolicyViolation> toleratedViolations)
	{
		return new MutationPolicyDecision(false, toleratedViolations, Optional.of(quarantineRequest));
	}

	public MutationPolicyDecision
	{
		Objects.requireNonNull(toleratedViolations, "toleratedViolations");
		Objects.requireNonNull(quarantineRequest, "quarantineRequest");
		if (allowed && quarantineRequest.isPresent())
		{
			throw new IllegalArgumentException("Allowed decisions must not carry quarantine requests");
		}
		toleratedViolations = List.copyOf(toleratedViolations);
	}

	public boolean quarantined()
	{
		return quarantineRequest.isPresent();
	}

	public MutationPolicyDecision withQuarantineRequest(final MutationQuarantineRequest quarantineRequest)
	{
		if (allowed)
		{
			throw new IllegalStateException("Allowed mutation decisions cannot carry quarantine requests");
		}
		return new MutationPolicyDecision(false, toleratedViolations, Optional.of(quarantineRequest));
	}
}
