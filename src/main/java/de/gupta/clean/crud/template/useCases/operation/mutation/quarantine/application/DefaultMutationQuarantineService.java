package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationResult;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.quarantine.MutationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.recording.MutationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.id.MutationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.port.persistence.MutationQuarantineRepository;

import java.time.Clock;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public final class DefaultMutationQuarantineService implements MutationQuarantineService
{
	private static final int REPLAY_SUMMARY_LIMIT = 2000;

	private final MutationQuarantineRepository repository;
	private final MutationQuarantineReplayRegistry replayRegistry;
	private final MutationQuarantineValueCodec valueCodec;
	private final Clock clock;

	public static DefaultMutationQuarantineService with(
			final MutationQuarantineRepository repository,
			final MutationQuarantineReplayRegistry replayRegistry,
			final MutationQuarantineValueCodec valueCodec,
			final Clock clock)
	{
		return new DefaultMutationQuarantineService(repository, replayRegistry, valueCodec, clock);
	}

	@Override
	public MutationQuarantineRequest record(final MutationQuarantineSubmission submission)
	{
		var request = submission.mutationRequest();
		var now = clock.instant();
		var serializedDomainId = valueCodec.serialize(request.domainId());
		var serializedPayload = valueCodec.serialize(request.payload());
		var record = new MutationQuarantineRecord(
				MutationQuarantineId.random(),
				submission.aggregateType(),
				serializedDomainId.valueType(),
				serializedDomainId.valueJson(),
				serializedPayload.valueType(),
				serializedPayload.valueJson(),
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
					valueCodec.deserialize(
							new SerializedMutationValue(record.domainIdType(), record.domainIdJson()),
							Object.class),
					valueCodec.deserialize(
							new SerializedMutationValue(record.payloadType(), record.payloadJson()),
							ApplicationOperationPayload.class),
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
			final MutationQuarantineValueCodec valueCodec,
			final Clock clock)
	{
		this.repository = Objects.requireNonNull(repository, "repository");
		this.replayRegistry = Objects.requireNonNull(replayRegistry, "replayRegistry");
		this.valueCodec = Objects.requireNonNull(valueCodec, "valueCodec");
		this.clock = Objects.requireNonNull(clock, "clock");
	}
}
