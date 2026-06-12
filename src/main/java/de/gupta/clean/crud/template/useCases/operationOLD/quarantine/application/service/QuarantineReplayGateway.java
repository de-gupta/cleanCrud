package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service;

public interface QuarantineReplayGateway<P>
{
	String aggregateKey();

	ReplayOutcome replay(QuarantineReplayCommand<P> command);
}