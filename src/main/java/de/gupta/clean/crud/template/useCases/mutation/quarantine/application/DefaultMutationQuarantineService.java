package de.gupta.clean.crud.template.useCases.mutation.quarantine.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.ApplicationMutationPayload;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationResult;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.quarantine.MutationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.application.recording.MutationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.MutationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.id.MutationQuarantineId;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.port.persistence.MutationQuarantineRepository;

import java.io.IOException;
import java.time.Clock;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public final class DefaultMutationQuarantineService implements MutationQuarantineService
{
	private static final int REPLAY_SUMMARY_LIMIT = 2000;

	private final MutationQuarantineRepository repository;
	private final MutationQuarantineReplayRegistry replayRegistry;
	private final ObjectMapper objectMapper;
	private final ObjectMapper payloadObjectMapper;
	private final Clock clock;

	public static DefaultMutationQuarantineService with(
			final MutationQuarantineRepository repository,
			final MutationQuarantineReplayRegistry replayRegistry,
			final ObjectMapper objectMapper,
			final Clock clock)
	{
		return new DefaultMutationQuarantineService(repository, replayRegistry, objectMapper, clock);
	}

	@Override
	public MutationQuarantineRequest record(final MutationQuarantineSubmission submission)
	{
		var request = submission.mutationRequest();
		var now = clock.instant();
		var record = new MutationQuarantineRecord(
				MutationQuarantineId.random(),
				submission.aggregateType(),
				request.domainId().getClass().getName(),
				serialize(request.domainId()),
				request.payloadType().getName(),
				serialize(request.payload()),
				request.source(),
				request.family(),
				request.correlationId(),
				request.causationId(),
				MutationQuarantineStatus.OPEN,
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
	public Optional<MutationQuarantineRecord> findById(final MutationQuarantineId quarantineId)
	{
		return repository.findById(quarantineId);
	}

	@Override
	public Collection<MutationQuarantineRecord> findOpen(final int limit)
	{
		return repository.findOpen(limit);
	}

	@Override
	public MutationQuarantineRecord dismiss(final MutationQuarantineId quarantineId)
	{
		var record = openRecord(quarantineId);
		return repository.update(record.dismissed(clock.instant()));
	}

	@Override
	public MutationQuarantineRecord replay(final MutationQuarantineId quarantineId)
	{
		var record = openRecord(quarantineId);
		var gateway = replayRegistry.findGateway(record.aggregateType())
		                            .orElseThrow(() -> InvalidRequestException.withMessage(
											"No mutation quarantine replay gateway registered for aggregate type "
													+ record.aggregateType()));
		try
		{
			var result = gateway.replay(new MutationQuarantineReplayCommand(
					quarantineId,
					deserialize(record.domainIdType(), record.domainIdJson()),
					(ApplicationMutationPayload) deserialize(record.payloadType(), record.payloadJson()),
					record.family(),
					record.correlationId(),
					record.causationId()));
			return persistReplayResult(record, result);
		}
		catch (RuntimeException e)
		{
			return repository.update(record.replayAttempted(
					clock.instant(),
					"FAILED",
					Optional.of(summaryFor(e))));
		}
	}

	private MutationQuarantineRecord persistReplayResult(
			final MutationQuarantineRecord record,
			final MutationResult<?, ?> result)
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

	private MutationQuarantineRecord openRecord(final MutationQuarantineId quarantineId)
	{
		var record = repository.findById(quarantineId)
		                       .orElseThrow(() -> ResourceNotFoundException.withId(quarantineId.value()));
		if (!record.open())
		{
			throw InvalidRequestException.withMessage(
					"Mutation quarantine " + quarantineId.value() + " is already " + record.status());
		}
		return record;
	}

	private String serialize(final Object value)
	{
		try
		{
			return payloadObjectMapper.writeValueAsString(value);
		}
		catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Failed to serialize mutation quarantine value", e);
		}
	}

	private Object deserialize(final String valueType, final String valueJson)
	{
		try
		{
			return payloadObjectMapper.readValue(valueJson, Class.forName(valueType));
		}
		catch (ClassNotFoundException | IOException e)
		{
			throw new IllegalStateException("Failed to deserialize mutation quarantine value", e);
		}
	}

	private String summaryFor(final RuntimeException e)
	{
		var message = Optional.ofNullable(e.getMessage()).orElse(e.getClass().getSimpleName());
		return trimmed(message);
	}

	private String trimmed(final String value)
	{
		return value.length() <= REPLAY_SUMMARY_LIMIT ? value : value.substring(0, REPLAY_SUMMARY_LIMIT);
	}

	private DefaultMutationQuarantineService(
			final MutationQuarantineRepository repository,
			final MutationQuarantineReplayRegistry replayRegistry,
			final ObjectMapper objectMapper,
			final Clock clock)
	{
		this.repository = Objects.requireNonNull(repository, "repository");
		this.replayRegistry = Objects.requireNonNull(replayRegistry, "replayRegistry");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.payloadObjectMapper = objectMapper.copy().disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS);
		this.clock = Objects.requireNonNull(clock, "clock");
	}
}