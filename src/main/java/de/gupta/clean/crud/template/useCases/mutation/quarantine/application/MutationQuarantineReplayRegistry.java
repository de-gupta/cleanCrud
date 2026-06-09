package de.gupta.clean.crud.template.useCases.mutation.quarantine.application;

import java.util.Optional;

public interface MutationQuarantineReplayRegistry
{
	Optional<MutationQuarantineReplayGateway> findGateway(String aggregateType);
}
