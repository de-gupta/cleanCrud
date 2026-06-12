package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.QuarantineReplayEnvelope;

import java.util.Objects;

public record CreationReplayInputs(QuarantineReplayEnvelope payload)
		implements PayloadReplayInputs
{
	public CreationReplayInputs
	{
		Objects.requireNonNull(payload, "payload");
	}
}