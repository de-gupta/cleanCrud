package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application;

import de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationResult;

public interface MutationQuarantineReplayGateway
{
	String aggregateType();

	MutationResult<?, ?> replay(MutationQuarantineReplayCommand command);
}
