package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.QuarantineReplayOutcome;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.QuarantineLifecycleRecord;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.port.QuarantineRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractQuarantineService<
		Id,
		Record extends QuarantineLifecycleRecord<Id, Record>,
		Submission,
		PersistedRequest>
{
	private static final int REPLAY_SUMMARY_LIMIT = 2000;

	private final QuarantineRepositoryPort<Id, Record> repository;
	private final Clock clock;
	private final QuarantineRecorder<Submission, Record, PersistedRequest> recorder;
	private final QuarantineReplayExecutor<Record> replayExecutor;

	public final PersistedRequest record(final Submission submission)
	{
		var now = clock.instant();
		var record = recorder.buildRecord(submission, now);
		repository.save(record);
		return recorder.persistedRequest(submission, record);
	}

	public final Optional<Record> findById(final Id quarantineId)
	{
		return repository.findById(quarantineId);
	}

	public final Collection<Record> findOpen(final int limit)
	{
		return repository.findOpen(limit);
	}

	public final Record dismiss(final Id quarantineId)
	{
		var record = openRecord(quarantineId);
		return repository.update(record.dismissed(clock.instant()));
	}

	public final Record replay(final Id quarantineId)
	{
		var record = openRecord(quarantineId);
		try
		{
			var outcome = replayExecutor.execute(record);
			return persistReplayResult(record, outcome);
		}
		catch (InvalidRequestException propagate)
		{
			throw propagate;
		}
		catch (RuntimeException caught)
		{
			return repository.update(record.replayAttempted(
					clock.instant(),
					QuarantineReplayOutcome.FAILED,
					Optional.of(summaryFor(caught))));
		}
	}

	protected final Instant now()
	{
		return clock.instant();
	}

	private Record openRecord(final Id quarantineId)
	{
		var record = repository.findById(quarantineId)
		                       .orElseThrow(() -> ResourceNotFoundException.withId(quarantineId));
		if (!record.open())
		{
			throw InvalidRequestException.withMessage(
					"Quarantine " + quarantineId + " is already " + record.statusLabel());
		}
		return record;
	}

	private Record persistReplayResult(final Record record, final ReplayOutcome outcome)
	{
		if (outcome.applied())
		{
			return repository.update(record.replayed(clock.instant()));
		}
		return repository.update(record.replayAttempted(
				clock.instant(),
				QuarantineReplayOutcome.QUARANTINED,
				outcome.quarantinedViolationSummary().map(this::trimmed)));
	}

	private String summaryFor(final RuntimeException caught)
	{
		var message = Optional.ofNullable(caught.getMessage()).orElse(caught.getClass().getSimpleName());
		return trimmed(message);
	}

	private String trimmed(final String value)
	{
		return value.length() <= REPLAY_SUMMARY_LIMIT ? value : value.substring(0, REPLAY_SUMMARY_LIMIT);
	}

	protected AbstractQuarantineService(
			final QuarantineRepositoryPort<Id, Record> repository,
			final Clock clock,
			final QuarantineRecorder<Submission, Record, PersistedRequest> recorder,
			final QuarantineReplayExecutor<Record> replayExecutor)
	{
		this.repository = Objects.requireNonNull(repository, "repository");
		this.clock = Objects.requireNonNull(clock, "clock");
		this.recorder = Objects.requireNonNull(recorder, "recorder");
		this.replayExecutor = Objects.requireNonNull(replayExecutor, "replayExecutor");
	}
}