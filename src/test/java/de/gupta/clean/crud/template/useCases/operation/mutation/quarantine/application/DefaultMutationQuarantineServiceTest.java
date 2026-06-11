package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application;

import de.gupta.clean.crud.template.useCases.operation.domain.model.*;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.violation.OperationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.quarantine.MutationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.recording.MutationQuarantineSubmission;
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

class DefaultMutationQuarantineServiceTest
{
	private QuarantineRecord<MutationReplayInputs> openRecord(final String aggregateType)
	{
		return new QuarantineRecord<>(
				new QuarantineId("quarantine-1"),
				aggregateType,
				new OperationInvocationMetadata(
						OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
						OperationFamily.APPLICATION,
						Optional.empty(),
						Optional.empty()),
				new MutationReplayInputs(
						QuarantineReplayEnvelope.of(String.class.getName(), "\"order-1\""),
						QuarantineReplayEnvelope.of(AcknowledgeOrder.class.getName(), "{\"value\":\"ack\"}")),
				List.of(OperationPolicyViolation.externalConsistency("broker mismatch")),
				QuarantineStatus.OPEN,
				Instant.parse("2026-06-09T10:15:00Z"),
				Instant.parse("2026-06-09T10:15:00Z"),
				0,
				Optional.empty(),
				Optional.empty(),
				Optional.empty());
	}

	private record AcknowledgeOrder(String value) implements ApplicationOperationPayload
	{
	}

	private static final class TestMutationQuarantineValueCodec implements QuarantineReplayCodec
	{
		@Override
		public QuarantineReplayEnvelope serialize(final Object value)
		{
			return switch (value)
			{
				case String domainId -> QuarantineReplayEnvelope.of(String.class.getName(), domainId);
				case AcknowledgeOrder payload ->
						QuarantineReplayEnvelope.of(AcknowledgeOrder.class.getName(), payload.value());
				default -> throw new IllegalArgumentException("Unsupported value " + value);
			};
		}

		@Override
		public <T> T deserialize(final QuarantineReplayEnvelope envelope, final Class<T> expectedType)
		{
			Object restored = switch (envelope.typeKey())
			{
				case "java.lang.String" -> envelope.serialized();
				case "de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.DefaultMutationQuarantineServiceTest$AcknowledgeOrder" ->
						new AcknowledgeOrder(envelope.serialized());
				default -> throw new IllegalArgumentException("Unsupported type " + envelope.typeKey());
			};
			return expectedType.cast(restored);
		}
	}

	private static final class InMemoryMutationQuarantineRepository
			implements QuarantineRepository<MutationReplayInputs>
	{
		private final Map<QuarantineId, QuarantineRecord<MutationReplayInputs>> records = new LinkedHashMap<>();

		@Override
		public QuarantineRecord<MutationReplayInputs> save(final QuarantineRecord<MutationReplayInputs> record)
		{
			records.put(record.quarantineId(), record);
			return record;
		}

		@Override
		public QuarantineRecord<MutationReplayInputs> update(final QuarantineRecord<MutationReplayInputs> record)
		{
			records.put(record.quarantineId(), record);
			return record;
		}

		@Override
		public Optional<QuarantineRecord<MutationReplayInputs>> findById(final QuarantineId quarantineId)
		{
			return Optional.ofNullable(records.get(quarantineId));
		}

		@Override
		public Collection<QuarantineRecord<MutationReplayInputs>> findOpen(final int limit)
		{
			return records.values().stream().filter(QuarantineRecord::open).limit(limit).toList();
		}
	}

	private static final class SuccessfulReplayGateway implements QuarantineReplayGateway<MutationReplayData>
	{
		@Override
		public String aggregateKey()
		{
			return "aggregate.OrderDefinition";
		}

		@Override
		public ReplayOutcome replay(final QuarantineReplayCommand<MutationReplayData> command)
		{
			return ReplayOutcome.success();
		}
	}

	private static final class FailingReplayGateway implements QuarantineReplayGateway<MutationReplayData>
	{
		@Override
		public String aggregateKey()
		{
			return "aggregate.OrderDefinition";
		}

		@Override
		public ReplayOutcome replay(final QuarantineReplayCommand<MutationReplayData> command)
		{
			throw new IllegalStateException("still inconsistent");
		}
	}

	@Nested
	class WhenRecordingSubmission
	{
		@Test
		void exposesPayloadTypeNameWithoutLaneSpecificCast()
		{
			assertThat(openRecord("aggregate.OrderDefinition").payloadTypeName())
					.as("generic quarantine record should expose payload type visibility")
					.isEqualTo(AcknowledgeOrder.class.getName());
		}

		@Test
		void assignsPersistentQuarantineId()
		{
			var service = DefaultMutationQuarantineService.with(
					new InMemoryMutationQuarantineRepository(),
					DefaultQuarantineReplayRegistry.of(List.of()),
					new TestMutationQuarantineValueCodec(),
					Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));

			var persisted = service.record(new MutationQuarantineSubmission(
					"aggregate.OrderDefinition",
					new MutationRequest<>("order-1", new AcknowledgeOrder("ack"),
							OperationSource.AUTHORITATIVE_EXTERNAL_EVENT),
					new MutationQuarantineRequest(
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
			assertThatThrownBy(() -> DefaultMutationQuarantineService.replayRegistry(List.of(
					new SuccessfulReplayGateway(),
					new SuccessfulReplayGateway())))
					.as("replay registry should reject duplicate aggregate keys")
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("Duplicate quarantine replay gateway");
		}

		@Test
		void marksRecordReplayedWhenGatewayAppliesMutation()
		{
			var repository = new InMemoryMutationQuarantineRepository();
			var service = DefaultMutationQuarantineService.with(
					repository,
					DefaultQuarantineReplayRegistry.of(List.of(new SuccessfulReplayGateway())),
					new TestMutationQuarantineValueCodec(),
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
			var repository = new InMemoryMutationQuarantineRepository();
			var service = DefaultMutationQuarantineService.with(
					repository,
					DefaultQuarantineReplayRegistry.of(List.of(new FailingReplayGateway())),
					new TestMutationQuarantineValueCodec(),
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
			var repository = new InMemoryMutationQuarantineRepository();
			var service = DefaultMutationQuarantineService.with(
					repository,
					DefaultQuarantineReplayRegistry.of(List.of()),
					new TestMutationQuarantineValueCodec(),
					Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));
			var dismissed = repository.save(openRecord("aggregate.OrderDefinition")
					.dismissed(Instant.parse("2026-06-09T10:16:00Z")));

			assertThatThrownBy(() -> service.dismiss(dismissed.quarantineId()))
					.as("dismissing an already-dismissed record should throw")
					.isInstanceOf(RuntimeException.class);
		}
	}
}
