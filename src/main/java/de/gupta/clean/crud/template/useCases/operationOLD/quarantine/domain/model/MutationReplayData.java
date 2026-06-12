package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;

import java.util.Objects;

public record MutationReplayData(Object domainId, ApplicationOperationPayload payload)
{
	public MutationReplayData
	{
		Objects.requireNonNull(domainId, "domainId");
		Objects.requireNonNull(payload, "payload");
	}
}