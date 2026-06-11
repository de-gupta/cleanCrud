package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.quarantine.MutationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.recording.MutationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.*;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.*;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.port.QuarantineRepository;

import java.time.Clock;
import java.util.Collection;
import java.util.Optional;

public final class DefaultMutationQuarantineService
		extends
		AbstractQuarantineService<QuarantineId, QuarantineRecord<MutationReplayInputs>, MutationQuarantineSubmission, MutationQuarantineRequest>
		implements QuarantineService<MutationReplayInputs>
{
	public static DefaultMutationQuarantineService with(
			final QuarantineRepository<MutationReplayInputs> repository,
			final QuarantineReplayRegistry<MutationReplayData> replayRegistry,
			final QuarantineReplayCodec replayCodec,
			final Clock clock)
	{
		return new DefaultMutationQuarantineService(repository, replayRegistry, replayCodec, clock);
	}

	public static QuarantineReplayRegistry<MutationReplayData> replayRegistry(
			final Collection<? extends QuarantineReplayGateway<MutationReplayData>> gateways)
	{
		return DefaultQuarantineReplayRegistry.of(gateways);
	}

	private static QuarantineRecorder<MutationQuarantineSubmission, QuarantineRecord<MutationReplayInputs>, MutationQuarantineRequest>
	recorder(final QuarantineReplayCodec replayCodec)
	{
		return new QuarantineRecorder<>()
		{
			@Override
			public QuarantineRecord<MutationReplayInputs> buildRecord(
					final MutationQuarantineSubmission submission,
					final java.time.Instant now)
			{
				var request = submission.mutationRequest();
				return new QuarantineRecord<>(
						QuarantineId.random(),
						submission.aggregateType(),
						new OperationInvocationMetadata(
								request.source(), request.family(),
								request.correlationId(), request.causationId()),
						new MutationReplayInputs(
								replayCodec.serialize(request.domainId()),
								replayCodec.serialize(request.payload())),
						submission.quarantineRequest().violations(),
						QuarantineStatus.OPEN,
						now, now, 0,
						Optional.empty(), Optional.empty(), Optional.empty());
			}

			@Override
			public MutationQuarantineRequest persistedRequest(
					final MutationQuarantineSubmission submission,
					final QuarantineRecord<MutationReplayInputs> savedRecord)
			{
				return submission.quarantineRequest().persistedAs(savedRecord.quarantineId());
			}
		};
	}

	private static QuarantineReplayExecutor<QuarantineRecord<MutationReplayInputs>> replayExecutor(
			final QuarantineReplayRegistry<MutationReplayData> replayRegistry,
			final QuarantineReplayCodec replayCodec)
	{
		return record ->
		{
			var gateway = replayRegistry.findGateway(record.aggregateKey())
			                            .orElseThrow(() -> InvalidRequestException.withMessage(
												"No mutation quarantine replay gateway registered for aggregate key "
														+ record.aggregateKey()));
			var domainId = replayCodec.deserialize(record.replayInputs().domainId(), Object.class);
			var payload = replayCodec.deserialize(
					record.replayInputs().payload(), ApplicationOperationPayload.class);
			return gateway.replay(new QuarantineReplayCommand<>(
					record.quarantineId(), new MutationReplayData(domainId, payload), record.metadata()));
		};
	}

	private DefaultMutationQuarantineService(
			final QuarantineRepository<MutationReplayInputs> repository,
			final QuarantineReplayRegistry<MutationReplayData> replayRegistry,
			final QuarantineReplayCodec replayCodec,
			final Clock clock)
	{
		super(repository, clock,
				recorder(replayCodec),
				replayExecutor(replayRegistry, replayCodec));
	}
}