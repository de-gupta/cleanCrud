package de.gupta.clean.crud.template.useCases.mutation.domain.policy.evaluation;

import de.gupta.clean.crud.template.useCases.mutation.domain.policy.quarantine.MutationQuarantineRequest;

import java.util.Objects;
import java.util.Optional;

public record MutationPolicyDecision(
		boolean allowed,
		Optional<MutationQuarantineRequest> quarantineRequest)
{
	public static MutationPolicyDecision allow()
	{
		return new MutationPolicyDecision(true, Optional.empty());
	}

	public static MutationPolicyDecision quarantine(final MutationQuarantineRequest quarantineRequest)
	{
		return new MutationPolicyDecision(false, Optional.of(quarantineRequest));
	}

	public MutationPolicyDecision
	{
		Objects.requireNonNull(quarantineRequest, "quarantineRequest");
		if (allowed && quarantineRequest.isPresent())
		{
			throw new IllegalArgumentException("Allowed decisions must not carry quarantine requests");
		}
	}

	public boolean quarantined()
	{
		return quarantineRequest.isPresent();
	}
}
