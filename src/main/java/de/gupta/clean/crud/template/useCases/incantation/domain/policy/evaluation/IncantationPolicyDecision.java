package de.gupta.clean.crud.template.useCases.incantation.domain.policy.evaluation;

import de.gupta.clean.crud.template.useCases.incantation.domain.policy.quarantine.IncantationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.violation.IncantationPolicyViolation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record IncantationPolicyDecision(
		boolean allowed,
		List<IncantationPolicyViolation> toleratedViolations,
		Optional<IncantationQuarantineRequest> quarantineRequest)
{
	public static IncantationPolicyDecision allow()
	{
		return allow(List.of());
	}

	public static IncantationPolicyDecision allow(final List<IncantationPolicyViolation> toleratedViolations)
	{
		return new IncantationPolicyDecision(true, toleratedViolations, Optional.empty());
	}

	public static IncantationPolicyDecision quarantine(final IncantationQuarantineRequest quarantineRequest)
	{
		return quarantine(quarantineRequest, List.of());
	}

	public static IncantationPolicyDecision quarantine(
			final IncantationQuarantineRequest quarantineRequest,
			final List<IncantationPolicyViolation> toleratedViolations)
	{
		return new IncantationPolicyDecision(false, toleratedViolations, Optional.of(quarantineRequest));
	}

	public IncantationPolicyDecision
	{
		toleratedViolations = List.copyOf(Objects.requireNonNull(toleratedViolations, "toleratedViolations"));
		Objects.requireNonNull(quarantineRequest, "quarantineRequest");
		if (allowed && quarantineRequest.isPresent())
		{
			throw new IllegalArgumentException("Allowed decisions cannot carry quarantine metadata");
		}
	}

	public boolean quarantined()
	{
		return quarantineRequest.isPresent();
	}
}
