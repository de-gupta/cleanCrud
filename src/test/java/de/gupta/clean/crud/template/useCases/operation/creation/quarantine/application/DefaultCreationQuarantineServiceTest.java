package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine.CreationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.recording.CreationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operation.domain.model.*;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.violation.OperationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.*;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.*;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.port.QuarantineRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultCreationQuarantineServiceTest
{
	private QuarantineRecord<CreationReplayInputs> openRecord(final String aggregateType)
	{
		return new QuarantineRecord<>(
				new QuarantineId("quarantine-1"),
				aggregateType,
				new OperationInvocationMetadata(
						OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
						OperationFamily.APPLICATION,
						Optional.empty(),
						Optional.empty()),
				new CreationReplayInputs(
						QuarantineReplayEnvelope.of(TestOperationPayload.class.getName(), "{\"value\":\"ack\"}")),
				List.of(OperationPolicyViolation.externalConsistency("broker mismatch")),
				QuarantineStatus.OPEN,
				Instant.parse("2026-06-09T10:15:00Z"),
				Instant.parse("2026-06-09T10:15:00Z"),
				0,
				Optional.empty(),
				Optional.empty(),
				Optional.empty());
	}

	private static final class TestCreationQuarantinePayloadCodec implements QuarantineReplayCodec
	{
		@Override
		public QuarantineReplayEnvelope serialize(final Object value)
		{
			var testPayload = (TestOperationPayload) value;
			return QuarantineReplayEnvelope.of(TestOperationPayload.class.getName(), testPayload.value());
		}

		@Override
		public <T> T deserialize(final QuarantineReplayEnvelope envelope, final Class<T> expectedType)
		{
			return expectedType.cast(new TestOperationPayload(envelope.serialized()));
		}
	}

	private static final class InMemoryCreationQuarantineRepository
			implements QuarantineRepository<CreationReplayInputs>
	{
		private final Map<QuarantineId, QuarantineRecord<CreationReplayInputs>> records = new LinkedHashMap<>();

		@Override
		public QuarantineRecord<CreationReplayInputs> save(final QuarantineRecord<CreationReplayInputs> record)
		{
			records.put(record.quarantineId(), record);
			return record;
		}

		@Override
		public QuarantineRecord<CreationReplayInputs> update(final QuarantineRecord<CreationReplayInputs> record)
		{
			records.put(record.quarantineId(), record);
			return record;
		}

		@Override
		public Optional<QuarantineRecord<CreationReplayInputs>> findById(final QuarantineId quarantineId)
		{
			return Optional.ofNullable(records.get(quarantineId));
		}

		@Override
		public Collection<QuarantineRecord<CreationReplayInputs>> findOpen(final int limit)
		{
			return records.values().stream().filter(QuarantineRecord::open).limit(limit).toList();
		}
	}

	private static final class SuccessfulReplayGateway
			implements QuarantineReplayGateway<ApplicationOperationPayload>
	{
		@Override
		public String aggregateKey()
		{
			return "aggregate.OrderDefinition";
		}

		@Override
		public ReplayOutcome replay(final QuarantineReplayCommand<ApplicationOperationPayload> command)
		{
			return ReplayOutcome.success();
		}
	}

	private static final class FailingReplayGateway
			implements QuarantineReplayGateway<ApplicationOperationPayload>
	{
		@Override
		public String aggregateKey()
		{
			return "aggregate.OrderDefinition";
		}

		@Override
		public ReplayOutcome replay(final QuarantineReplayCommand<ApplicationOperationPayload> command)
		{
			throw new IllegalStateException("still inconsistent");
		}
	}

	@Nested
	class WhenRecordingSubmission
	{
		@Test
		void assignsPersistentQuarantineId()
		{
			var service = DefaultCreationQuarantineService.with(
					new InMemoryCreationQuarantineRepository(),
					DefaultQuarantineReplayRegistry.of(List.of()),
					new TestCreationQuarantinePayloadCodec(),
					Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));

			var persisted = service.record(new CreationQuarantineSubmission(
					"aggregate.OrderDefinition",
					new CreationRequest<>(new TestOperationPayload("ack"),
							OperationSource.AUTHORITATIVE_EXTERNAL_EVENT),
					new CreationQuarantineRequest(
							OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
							List.of(OperationPolicyViolation.externalConsistency("broker mismatch")))));

			assertThat(persisted.quarantineId())
					.as("recorded submission should carry a persisted quarantine id")
					.isPresent();
		}
	}

	@Nested
	class WhenReplaying
	{
		@Test
		void registryRejectsDuplicateAggregateKeys()
		{
			assertThatThrownBy(() -> DefaultCreationQuarantineService.replayRegistry(List.of(
					new SuccessfulReplayGateway(),
					new SuccessfulReplayGateway())))
					.as("replay registry should reject duplicate aggregate keys")
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("Duplicate quarantine replay gateway");
		}

		@Test
		void marksRecordReplayedWhenGatewayAppliesCreation()
		{
			var repository = new InMemoryCreationQuarantineRepository();
			var service = DefaultCreationQuarantineService.with(
					repository,
					DefaultQuarantineReplayRegistry.of(List.of(new SuccessfulReplayGateway())),
					new TestCreationQuarantinePayloadCodec(),
					Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));
			var record = repository.save(openRecord("aggregate.OrderDefinition"));

			var replayed = service.replay(record.quarantineId());

			assertThat(replayed.status())
					.as("status should be REPLAYED after successful gateway replay")
					.isEqualTo(QuarantineStatus.REPLAYED);
			assertThat(replayed.replayAttemptCount())
					.as("replay attempt count should increment after replay")
					.isEqualTo(1);
		}

		@Test
		void leavesRecordOpenWhenGatewayRejects()
		{
			var repository = new InMemoryCreationQuarantineRepository();
			var service = DefaultCreationQuarantineService.with(
					repository,
					DefaultQuarantineReplayRegistry.of(List.of(new FailingReplayGateway())),
					new TestCreationQuarantinePayloadCodec(),
					Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));
			var record = repository.save(openRecord("aggregate.OrderDefinition"));

			var replayed = service.replay(record.quarantineId());

			assertThat(replayed.status())
					.as("status should remain OPEN when gateway throws")
					.isEqualTo(QuarantineStatus.OPEN);
			assertThat(replayed.lastReplayOutcome())
					.as("last replay outcome should contain FAILED when gateway throws")
					.contains(QuarantineReplayOutcome.FAILED);
		}
	}

	@Nested
	class WhenDismissing
	{
		@Test
		void rejectsAlreadyResolvedRecords()
		{
			var repository = new InMemoryCreationQuarantineRepository();
			var service = DefaultCreationQuarantineService.with(
					repository,
					DefaultQuarantineReplayRegistry.of(List.of()),
					new TestCreationQuarantinePayloadCodec(),
					Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));
			var dismissed = repository.save(openRecord("aggregate.OrderDefinition")
					.dismissed(Instant.parse("2026-06-09T10:16:00Z")));

			assertThatThrownBy(() -> service.dismiss(dismissed.quarantineId()))
					.as("dismissing an already-dismissed record should throw")
					.isInstanceOf(RuntimeException.class);
		}
	}
}
