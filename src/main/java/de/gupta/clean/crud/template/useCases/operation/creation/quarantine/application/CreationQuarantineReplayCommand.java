package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application;

import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;

import java.util.Objects;
import java.util.Optional;

public record CreationQuarantineReplayCommand(
		CreationQuarantineId quarantineId,
		ApplicationOperationPayload payload,
		OperationFamily family,
		Optional<OperationCorrelationId> correlationId,
		Optional<OperationCausationId> causationId)
{
	public CreationQuarantineReplayCommand
	{
		Objects.requireNonNull(quarantineId, "quarantineId");
		Objects.requireNonNull(payload, "payload");
		Objects.requireNonNull(family, "family");
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(causationId, "causationId");
	}
}
