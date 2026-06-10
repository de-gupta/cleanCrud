package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayEnvelope;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.quarantine.MutationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.recording.MutationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.id.MutationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.port.persistence.MutationQuarantineRepository;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.AbstractQuarantineService;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.QuarantineRecorder;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.QuarantineReplayExecutor;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.ReplayOutcome;

import java.time.Clock;
import java.util.Optional;

public final class DefaultMutationQuarantineService
		extends
		AbstractQuarantineService<MutationQuarantineId, MutationQuarantineRecord, MutationQuarantineSubmission, MutationQuarantineRequest>
		implements MutationQuarantineService
{
	public static DefaultMutationQuarantineService with(
			final MutationQuarantineRepository repository,
			final MutationQuarantineReplayRegistry replayRegistry,
			final MutationQuarantineValueCodec valueCodec,
			final Clock clock)
	{
		return new DefaultMutationQuarantineService(repository, replayRegistry, valueCodec, clock);
	}

	private static QuarantineRecorder<MutationQuarantineSubmission, MutationQuarantineRecord, MutationQuarantineRequest>
	recorder(final MutationQuarantineValueCodec valueCodec)
	{
		return new QuarantineRecorder<>()
		{
			@Override
			public MutationQuarantineRecord buildRecord(
					final MutationQuarantineSubmission submission,
					final java.time.Instant now)
			{
				var request = submission.mutationRequest();
				var serializedDomainId = valueCodec.serialize(request.domainId());
				var serializedPayload = valueCodec.serialize(request.payload());
				return new MutationQuarantineRecord(
						MutationQuarantineId.random(),
						submission.aggregateType(),
						QuarantineReplayEnvelope.of(serializedDomainId.valueType(), serializedDomainId.valueJson()),
						QuarantineReplayEnvelope.of(serializedPayload.valueType(), serializedPayload.valueJson()),
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
			}

			@Override
			public MutationQuarantineRequest persistedRequest(
					final MutationQuarantineSubmission submission,
					final MutationQuarantineRecord savedRecord)
			{
				return submission.quarantineRequest().persistedAs(savedRecord.quarantineId());
			}
		};
	}

	private static QuarantineReplayExecutor<MutationQuarantineRecord> replayExecutor(
			final MutationQuarantineReplayRegistry replayRegistry,
			final MutationQuarantineValueCodec valueCodec)
	{
		return record ->
		{
			var gateway = replayRegistry.findGateway(record.aggregateType())
			                            .orElseThrow(() -> InvalidRequestException.withMessage(
												"No mutation quarantine replay gateway registered for aggregate type "
														+ record.aggregateType()));
			var result = gateway.replay(new MutationQuarantineReplayCommand(
					record.quarantineId(),
					valueCodec.deserialize(
							new SerializedMutationValue(record.domainId().typeKey(), record.domainId().serialized()),
							Object.class),
					valueCodec.deserialize(
							new SerializedMutationValue(record.payload().typeKey(), record.payload().serialized()),
							ApplicationOperationPayload.class),
					record.family(),
					record.correlationId(),
					record.causationId()));
			if (result.applied())
			{
				return ReplayOutcome.success();
			}
			return ReplayOutcome.quarantined(
					result.quarantineRequest().map(r -> r.violations().toString()));
		};
	}

	private DefaultMutationQuarantineService(
			final MutationQuarantineRepository repository,
			final MutationQuarantineReplayRegistry replayRegistry,
			final MutationQuarantineValueCodec valueCodec,
			final Clock clock)
	{
		super(repository, clock,
				recorder(valueCodec),
				replayExecutor(replayRegistry, valueCodec));
	}
}