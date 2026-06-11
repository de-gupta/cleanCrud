package de.gupta.clean.crud.template.useCases.operation.quarantine.application.service;

import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.OperationInvocationMetadata;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.QuarantineId;

import java.util.Objects;

public record QuarantineReplayCommand<P>(
		QuarantineId quarantineId,
		P replayInputs,
		OperationInvocationMetadata metadata)
{
	public QuarantineReplayCommand
	{
		Objects.requireNonNull(quarantineId, "quarantineId");
		Objects.requireNonNull(replayInputs, "replayInputs");
		Objects.requireNonNull(metadata, "metadata");
	}
}
