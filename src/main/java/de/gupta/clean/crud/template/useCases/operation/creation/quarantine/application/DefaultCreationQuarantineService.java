package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationResult;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine.CreationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.recording.CreationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.port.persistence.CreationQuarantineRepository;

import java.time.Clock;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public final class DefaultCreationQuarantineService implements CreationQuarantineService
{
	private static final int REPLAY_SUMMARY_LIMIT = 2000;

	private final CreationQuarantineRepository repository;
	private final CreationQuarantineReplayRegistry replayRegistry;
	private final CreationQuarantinePayloadCodec payloadCodec;
	private final Clock clock;

	public static DefaultCreationQuarantineService with(
			final CreationQuarantineRepository repository,
			final CreationQuarantineReplayRegistry replayRegistry,
			final CreationQuarantinePayloadCodec payloadCodec,
			final Clock clock)
	{
		return new DefaultCreationQuarantineService(repository, replayRegistry, payloadCodec, clock);
	}

	@Override
	public CreationQuarantineRequest record(final CreationQuarantineSubmission submission)
	{
		var request = submission.creationRequest();
		var now = clock.instant();
		var serializedPayload = payloadCodec.serialize(request.payload());
		var record = new CreationQuarantineRecord(
				CreationQuarantineId.random(),
				submission.aggregateType(),
				serializedPayload.payloadType(),
				serializedPayload.payloadJson(),
				request.source(),
				request.family(),
				request.correlationId(),
				request.causationId(),
				CreationQuarantineStatus.OPEN,
				submission.quarantineRequest().violations(),
				now,
				now,
				0,
				Optional.empty(),
				Optional.empty(),
				Optional.empty());
		repository.save(record);
		return submission.quarantineRequest().persistedAs(record.quarantineId());
	}

	@Override
	public Optional<CreationQuarantineRecord> findById(final CreationQuarantineId quarantineId)
	{
		return repository.findById(quarantineId);
	}

	@Override
	public Collection<CreationQuarantineRecord> findOpen(final int limit)
	{
		return repository.findOpen(limit);
	}

	@Override
	public CreationQuarantineRecord dismiss(final CreationQuarantineId quarantineId)
	{
		var record = openRecord(quarantineId);
		return repository.update(record.dismissed(clock.instant()));
	}

	@Override
	public CreationQuarantineRecord replay(final CreationQuarantineId quarantineId)
	{
		var record = openRecord(quarantineId);
		var gateway = replayRegistry.findGateway(record.aggregateType())
		                            .orElseThrow(() -> InvalidRequestException.withMessage(
											"No creation quarantine replay gateway registered for aggregate type "
													+ record.aggregateType()));
		try
		{
			var result = gateway.replay(new CreationQuarantineReplayCommand(
					quarantineId,
					payloadCodec.deserialize(new SerializedCreationPayload(record.payloadType(), record.payloadJson())),
					record.family(),
					record.correlationId(),
					record.causationId()));
			return persistReplayResult(record, result);
		}
		catch (RuntimeException caught)
		{
			return repository.update(record.replayAttempted(
					clock.instant(),
					"FAILED",
					Optional.of(summaryFor(caught))));
		}
	}

	private CreationQuarantineRecord persistReplayResult(
			final CreationQuarantineRecord record,
			final CreationResult<?, ?> result)
	{
		if (result.applied())
		{
			return repository.update(record.replayed(clock.instant()));
		}
		return repository.update(record.replayAttempted(
				clock.instant(),
				"QUARANTINED",
				result.quarantineRequest()
				      .map(request -> request.violations().toString())
				      .map(this::trimmed)));
	}

	private CreationQuarantineRecord openRecord(final CreationQuarantineId quarantineId)
	{
		var record = repository.findById(quarantineId)
		                       .orElseThrow(() -> ResourceNotFoundException.withId(quarantineId.value()));
		if (!record.open())
		{
			throw InvalidRequestException.withMessage(
					"Creation quarantine " + quarantineId.value() + " is already " + record.status());
		}
		return record;
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

	private DefaultCreationQuarantineService(
			final CreationQuarantineRepository repository,
			final CreationQuarantineReplayRegistry replayRegistry,
			final CreationQuarantinePayloadCodec payloadCodec,
			final Clock clock)
	{
		this.repository = Objects.requireNonNull(repository, "repository");
		this.replayRegistry = Objects.requireNonNull(replayRegistry, "replayRegistry");
		this.payloadCodec = Objects.requireNonNull(payloadCodec, "payloadCodec");
		this.clock = Objects.requireNonNull(clock, "clock");
	}
}