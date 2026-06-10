package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application;

import java.util.Optional;

public interface CreationQuarantineReplayRegistry
{
	Optional<CreationQuarantineReplayGateway> findGateway(String aggregateType);
}
