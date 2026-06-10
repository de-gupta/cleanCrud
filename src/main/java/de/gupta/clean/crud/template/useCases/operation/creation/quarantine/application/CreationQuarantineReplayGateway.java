package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationResult;

public interface CreationQuarantineReplayGateway
{
	String aggregateType();

	CreationResult<?, ?> replay(CreationQuarantineReplayCommand command);
}
