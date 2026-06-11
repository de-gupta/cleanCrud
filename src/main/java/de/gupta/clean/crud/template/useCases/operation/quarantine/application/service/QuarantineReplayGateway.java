package de.gupta.clean.crud.template.useCases.operation.quarantine.application.service;

public interface QuarantineReplayGateway<P>
{
	String aggregateKey();

	ReplayOutcome replay(QuarantineReplayCommand<P> command);
}
