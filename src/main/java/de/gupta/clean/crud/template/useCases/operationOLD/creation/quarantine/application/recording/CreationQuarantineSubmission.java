package de.gupta.clean.crud.template.useCases.operationOLD.creation.quarantine.application.recording;

import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.model.CreationRequest;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.quarantine.CreationQuarantineRequest;

import java.util.Objects;

public record CreationQuarantineSubmission(
		String aggregateType,
		CreationRequest<?> creationRequest,
		CreationQuarantineRequest quarantineRequest)
{
	public CreationQuarantineSubmission
	{
		Objects.requireNonNull(aggregateType, "aggregateType");
		Objects.requireNonNull(creationRequest, "creationRequest");
		Objects.requireNonNull(quarantineRequest, "quarantineRequest");
	}
}