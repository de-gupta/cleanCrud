package de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.evaluation;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine.CreationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.violation.OperationPolicyViolation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CreationPolicyDecision(
		boolean allowed,
		List<OperationPolicyViolation> toleratedViolations,
		Optional<CreationQuarantineRequest> quarantineRequest)
{
	public static CreationPolicyDecision allow()
	{
		return allow(List.of());
	}

	public static CreationPolicyDecision allow(final List<OperationPolicyViolation> toleratedViolations)
	{
		return new CreationPolicyDecision(true, toleratedViolations, Optional.empty());
	}

	public static CreationPolicyDecision quarantine(final CreationQuarantineRequest quarantineRequest)
	{
		return quarantine(quarantineRequest, List.of());
	}

	public static CreationPolicyDecision quarantine(
			final CreationQuarantineRequest quarantineRequest,
			final List<OperationPolicyViolation> toleratedViolations)
	{
		return new CreationPolicyDecision(false, toleratedViolations, Optional.of(quarantineRequest));
	}

	public CreationPolicyDecision
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

	public CreationPolicyDecision withQuarantineRequest(final CreationQuarantineRequest quarantineRequest)
	{
		if (allowed)
		{
			throw new IllegalStateException("Allowed creation decisions cannot carry quarantine requests");
		}
		return new CreationPolicyDecision(false, toleratedViolations, Optional.of(quarantineRequest));
	}
}
