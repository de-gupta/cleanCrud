package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine.CreationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.recording.CreationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.port.persistence.CreationQuarantineRepository;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.*;

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
			final QuarantineReplayCodec replayCodec,
			final Clock clock)
	{
		return new DefaultCreationQuarantineService(repository, replayRegistry, replayCodec, clock);
	}

	private static QuarantineRecorder<CreationQuarantineSubmission, CreationQuarantineRecord, CreationQuarantineRequest>
	recorder(final QuarantineReplayCodec replayCodec)
	{
		return new QuarantineRecorder<>()
		{
			@Override
			public CreationQuarantineRecord buildRecord(
					final CreationQuarantineSubmission submission,
					final java.time.Instant now)
			{
				var request = submission.creationRequest();
				return new CreationQuarantineRecord(
						CreationQuarantineId.random(),
						submission.aggregateType(),
						replayCodec.serialize(request.payload()),
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
			final QuarantineReplayCodec replayCodec)
	{
		return record ->
		{
			var gateway = replayRegistry.findGateway(record.aggregateType())
			                            .orElseThrow(() -> InvalidRequestException.withMessage(
												"No creation quarantine replay gateway registered for aggregate type "
														+ record.aggregateType()));
			var result = gateway.replay(new CreationQuarantineReplayCommand(
					record.quarantineId(),
					replayCodec.deserialize(record.payload(), ApplicationOperationPayload.class),
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
			final QuarantineReplayCodec replayCodec,
			final Clock clock)
	{
		super(repository, clock,
				recorder(replayCodec),
				replayExecutor(replayRegistry, replayCodec));
	}
}