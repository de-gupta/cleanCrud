package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine.CreationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.recording.CreationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.port.persistence.CreationQuarantineRepository;
import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayEnvelope;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.AbstractQuarantineService;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.QuarantineRecorder;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.QuarantineReplayExecutor;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.ReplayOutcome;

import java.time.Clock;
import java.util.Optional;

public final class DefaultCreationQuarantineService
		extends
		AbstractQuarantineService<CreationQuarantineId, CreationQuarantineRecord, CreationQuarantineSubmission, CreationQuarantineRequest>
		implements CreationQuarantineService
{
	public static DefaultCreationQuarantineService with(
			final CreationQuarantineRepository repository,
			final CreationQuarantineReplayRegistry replayRegistry,
			final CreationQuarantinePayloadCodec payloadCodec,
			final Clock clock)
	{
		return new DefaultCreationQuarantineService(repository, replayRegistry, payloadCodec, clock);
	}

	private static QuarantineRecorder<CreationQuarantineSubmission, CreationQuarantineRecord, CreationQuarantineRequest>
	recorder(final CreationQuarantinePayloadCodec payloadCodec)
	{
		return new QuarantineRecorder<>()
		{
			@Override
			public CreationQuarantineRecord buildRecord(
					final CreationQuarantineSubmission submission,
					final java.time.Instant now)
			{
				var request = submission.creationRequest();
				var serialized = payloadCodec.serialize(request.payload());
				return new CreationQuarantineRecord(
						CreationQuarantineId.random(),
						submission.aggregateType(),
						QuarantineReplayEnvelope.of(serialized.payloadType(), serialized.payloadJson()),
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
			}

			@Override
			public CreationQuarantineRequest persistedRequest(
					final CreationQuarantineSubmission submission,
					final CreationQuarantineRecord savedRecord)
			{
				return submission.quarantineRequest().persistedAs(savedRecord.quarantineId());
			}
		};
	}

	private static QuarantineReplayExecutor<CreationQuarantineRecord> replayExecutor(
			final CreationQuarantineReplayRegistry replayRegistry,
			final CreationQuarantinePayloadCodec payloadCodec)
	{
		return record ->
		{
			var gateway = replayRegistry.findGateway(record.aggregateType())
			                            .orElseThrow(() -> InvalidRequestException.withMessage(
												"No creation quarantine replay gateway registered for aggregate type "
														+ record.aggregateType()));
			var result = gateway.replay(new CreationQuarantineReplayCommand(
					record.quarantineId(),
					payloadCodec.deserialize(
							new SerializedCreationPayload(record.payload().typeKey(), record.payload().serialized())),
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

	private DefaultCreationQuarantineService(
			final CreationQuarantineRepository repository,
			final CreationQuarantineReplayRegistry replayRegistry,
			final CreationQuarantinePayloadCodec payloadCodec,
			final Clock clock)
	{
		super(repository, clock,
				recorder(payloadCodec),
				replayExecutor(replayRegistry, payloadCodec));
	}
}