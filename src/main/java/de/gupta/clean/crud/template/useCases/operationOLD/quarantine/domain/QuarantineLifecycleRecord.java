package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.QuarantineReplayOutcome;

import java.time.Instant;
import java.util.Optional;

public interface QuarantineLifecycleRecord<Id, Self extends QuarantineLifecycleRecord<Id, Self>>
{
	Id quarantineId();

	String aggregateType();

	boolean open();

	String statusLabel();

	Self dismissed(Instant at);

	Self replayed(Instant at);

	Self replayAttempted(Instant at, QuarantineReplayOutcome outcome, Optional<String> summary);
}