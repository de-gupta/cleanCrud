package de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model;

import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayEnvelope;

import java.util.Objects;

public record MutationReplayInputs(
		QuarantineReplayEnvelope domainId,
		QuarantineReplayEnvelope payload)
{
	public MutationReplayInputs
	{
		Objects.requireNonNull(domainId, "domainId");
		Objects.requireNonNull(payload, "payload");
	}
}
