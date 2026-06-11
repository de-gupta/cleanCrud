package de.gupta.clean.crud.template.useCases.operation.quarantine.application.service;

import java.util.Optional;

public interface QuarantineReplayRegistry<P>
{
	Optional<QuarantineReplayGateway<P>> findGateway(String aggregateKey);
}
