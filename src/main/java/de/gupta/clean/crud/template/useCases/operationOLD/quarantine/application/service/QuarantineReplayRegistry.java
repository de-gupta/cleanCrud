package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service;

import java.util.Optional;

public interface QuarantineReplayRegistry<P>
{
	Optional<QuarantineReplayGateway<P>> findGateway(String aggregateKey);
}