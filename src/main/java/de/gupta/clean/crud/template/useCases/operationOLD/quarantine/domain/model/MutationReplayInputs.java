package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.QuarantineReplayEnvelope;

import java.util.Objects;

public record MutationReplayInputs(
		QuarantineReplayEnvelope domainId,
		QuarantineReplayEnvelope payload)
		implements PayloadReplayInputs
{
	public MutationReplayInputs
	{
		Objects.requireNonNull(domainId, "domainId");
		Objects.requireNonNull(payload, "payload");
	}
}