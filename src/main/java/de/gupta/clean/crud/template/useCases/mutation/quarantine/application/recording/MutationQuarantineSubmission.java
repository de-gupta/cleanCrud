package de.gupta.clean.crud.template.useCases.mutation.quarantine.application.recording;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.quarantine.MutationQuarantineRequest;

import java.util.Objects;

public record MutationQuarantineSubmission(
		String aggregateType,
		MutationRequest<?, ?> mutationRequest,
		MutationQuarantineRequest quarantineRequest)
{
	public MutationQuarantineSubmission
	{
		Objects.requireNonNull(aggregateType, "aggregateType");
		Objects.requireNonNull(mutationRequest, "mutationRequest");
		Objects.requireNonNull(quarantineRequest, "quarantineRequest");
	}
}
