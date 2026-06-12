package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service;

@FunctionalInterface
public interface QuarantineReplayExecutor<Record>
{
	ReplayOutcome execute(Record record);
}