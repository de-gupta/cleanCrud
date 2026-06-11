package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine.CreationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.recording.CreationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.*;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.*;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.port.QuarantineRepository;

import java.time.Clock;
import java.util.Collection;
import java.util.Optional;

public final class DefaultCreationQuarantineService
		extends
		AbstractQuarantineService<QuarantineId, QuarantineRecord<CreationReplayInputs>, CreationQuarantineSubmission, CreationQuarantineRequest>
		implements QuarantineService<CreationReplayInputs>
{
	public static DefaultCreationQuarantineService with(
			final QuarantineRepository<CreationReplayInputs> repository,
			final QuarantineReplayRegistry<ApplicationOperationPayload> replayRegistry,
			final QuarantineReplayCodec replayCodec,
			final Clock clock)
	{
		return new DefaultCreationQuarantineService(repository, replayRegistry, replayCodec, clock);
	}

	public static QuarantineReplayRegistry<ApplicationOperationPayload> replayRegistry(
			final Collection<? extends QuarantineReplayGateway<ApplicationOperationPayload>> gateways)
	{
		return DefaultQuarantineReplayRegistry.of(gateways);
	}

	private static QuarantineRecorder<CreationQuarantineSubmission, QuarantineRecord<CreationReplayInputs>, CreationQuarantineRequest>
	recorder(final QuarantineReplayCodec replayCodec)
	{
		return new QuarantineRecorder<>()
		{
			@Override
			public QuarantineRecord<CreationReplayInputs> buildRecord(
					final CreationQuarantineSubmission submission,
					final java.time.Instant now)
			{
				var request = submission.creationRequest();
				return new QuarantineRecord<>(
						QuarantineId.random(),
						submission.aggregateType(),
						new OperationInvocationMetadata(
								request.source(), request.family(),
								request.correlationId(), request.causationId()),
						new CreationReplayInputs(replayCodec.serialize(request.payload())),
						submission.quarantineRequest().violations(),
						QuarantineStatus.OPEN,
						now, now, 0,
						Optional.empty(), Optional.empty(), Optional.empty());
			}

			@Override
			public CreationQuarantineRequest persistedRequest(
					final CreationQuarantineSubmission submission,
					final QuarantineRecord<CreationReplayInputs> savedRecord)
			{
				return submission.quarantineRequest().persistedAs(savedRecord.quarantineId());
			}
		};
	}

	private static QuarantineReplayExecutor<QuarantineRecord<CreationReplayInputs>> replayExecutor(
			final QuarantineReplayRegistry<ApplicationOperationPayload> replayRegistry,
			final QuarantineReplayCodec replayCodec)
	{
		return record ->
		{
			var gateway = replayRegistry.findGateway(record.aggregateKey())
			                            .orElseThrow(() -> InvalidRequestException.withMessage(
												"No creation quarantine replay gateway registered for aggregate key "
														+ record.aggregateKey()));
			var decodedPayload = replayCodec.deserialize(
					record.replayInputs().payload(), ApplicationOperationPayload.class);
			return gateway.replay(new QuarantineReplayCommand<>(
					record.quarantineId(), decodedPayload, record.metadata()));
		};
	}

	private DefaultCreationQuarantineService(
			final QuarantineRepository<CreationReplayInputs> repository,
			final QuarantineReplayRegistry<ApplicationOperationPayload> replayRegistry,
			final QuarantineReplayCodec replayCodec,
			final Clock clock)
	{
		super(repository, clock,
				recorder(replayCodec),
				replayExecutor(replayRegistry, replayCodec));
	}
}