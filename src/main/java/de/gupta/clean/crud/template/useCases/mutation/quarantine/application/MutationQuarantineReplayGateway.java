package de.gupta.clean.crud.template.useCases.mutation.quarantine.application;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationResult;

public interface MutationQuarantineReplayGateway
{
	String aggregateType();

	MutationResult<?, ?> replay(MutationQuarantineReplayCommand command);
}
