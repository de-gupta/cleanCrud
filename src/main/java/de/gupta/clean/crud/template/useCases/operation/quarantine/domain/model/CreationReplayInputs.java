package de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model;

import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayEnvelope;

import java.util.Objects;

public record CreationReplayInputs(QuarantineReplayEnvelope payload)
		implements PayloadReplayInputs
{
	public CreationReplayInputs
	{
		Objects.requireNonNull(payload, "payload");
	}
}
