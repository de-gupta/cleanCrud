package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application;

import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayOutcome;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationResult;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.evaluation.MutationPolicyDecision;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.quarantine.MutationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.violation.MutationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.recording.MutationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.id.MutationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.port.persistence.MutationQuarantineRepository;
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
	private MutationQuarantineRecord openRecord(final String aggregateType)
	{
		return new MutationQuarantineRecord(
				new MutationQuarantineId("quarantine-1"),
				aggregateType,
				String.class.getName(),
				"\"order-1\"",
				AcknowledgeOrder.class.getName(),
				"{\"value\":\"ack\"}",
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
				OperationFamily.APPLICATION,
				Optional.empty(),
				Optional.empty(),
				MutationQuarantineStatus.OPEN,
				List.of(MutationPolicyViolation.externalConsistency("broker mismatch")),
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

	private static final class TestMutationQuarantineValueCodec implements MutationQuarantineValueCodec
	{
		@Override
		public SerializedMutationValue serialize(final Object value)
		{
			return switch (value)
			{
				case String domainId -> new SerializedMutationValue(String.class.getName(), domainId);
				case AcknowledgeOrder payload -> new SerializedMutationValue(AcknowledgeOrder.class.getName(),
						payload.value());
				default -> throw new IllegalArgumentException("Unsupported value " + value);
			};
		}

		@Override
		public <T> T deserialize(final SerializedMutationValue value, final Class<T> expectedType)
		{
			Object restored = switch (value.valueType())
			{
				case "java.lang.String" -> value.valueJson();
				case "de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.DefaultMutationQuarantineServiceTest$AcknowledgeOrder" ->
						new AcknowledgeOrder(value.valueJson());
				default -> throw new IllegalArgumentException("Unsupported value type " + value.valueType());
			};
			return expectedType.cast(restored);
		}
	}

	private static final class InMemoryMutationQuarantineRepository implements MutationQuarantineRepository
	{
		private final Map<MutationQuarantineId, MutationQuarantineRecord> records = new LinkedHashMap<>();

		@Override
		public MutationQuarantineRecord save(final MutationQuarantineRecord record)
		{
			records.put(record.quarantineId(), record);
			return record;
		}

		@Override
		public MutationQuarantineRecord update(final MutationQuarantineRecord record)
		{
			records.put(record.quarantineId(), record);
			return record;
		}

		@Override
		public Optional<MutationQuarantineRecord> findById(final MutationQuarantineId quarantineId)
		{
			return Optional.ofNullable(records.get(quarantineId));
		}

		@Override
		public Collection<MutationQuarantineRecord> findOpen(final int limit)
		{
			return records.values().stream().filter(MutationQuarantineRecord::open).limit(limit).toList();
		}
	}

	private static final class SuccessfulReplayGateway implements MutationQuarantineReplayGateway
	{
		@Override
		public String aggregateType()
		{
			return "aggregate.OrderDefinition";
		}

		@Override
		public MutationResult<?, ?> replay(final MutationQuarantineReplayCommand command)
		{
			return MutationResult.applied(
					new de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationContext<>(
							command.domainId(),
							OperationSource.ADMINISTRATIVE_REPLAY,
							command.family(),
							command.payload().getClass(),
							command.correlationId(),
							command.causationId(),
							Optional.empty(),
							Optional.of("ACKNOWLEDGED")),
					MutationPolicyDecision.allow(),
					de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel.of(
							command.domainId(),
							"ACKNOWLEDGED"));
		}
	}

	private static final class FailingReplayGateway implements MutationQuarantineReplayGateway
	{
		@Override
		public String aggregateType()
		{
			return "aggregate.OrderDefinition";
		}

		@Override
		public MutationResult<?, ?> replay(final MutationQuarantineReplayCommand command)
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
			var service = DefaultMutationQuarantineService.with(
					new InMemoryMutationQuarantineRepository(),
					DefaultMutationQuarantineReplayRegistry.of(List.of()),
					new TestMutationQuarantineValueCodec(),
					Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));

			var persisted = service.record(new MutationQuarantineSubmission(
					"aggregate.OrderDefinition",
					new MutationRequest<>("order-1", new AcknowledgeOrder("ack"),
							OperationSource.AUTHORITATIVE_EXTERNAL_EVENT),
					new MutationQuarantineRequest(
							OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
							List.of(MutationPolicyViolation.externalConsistency("broker mismatch")))));

			assertThat(persisted.quarantineId())
					.as("recorded submission should carry a persisted quarantine id")
					.isPresent();
		}
	}

	@Nested
	class WhenReplaying
	{
		@Test
		void marksRecordReplayedWhenGatewayAppliesMutation()
		{
			var repository = new InMemoryMutationQuarantineRepository();
			var service = DefaultMutationQuarantineService.with(
					repository,
					DefaultMutationQuarantineReplayRegistry.of(List.of(new SuccessfulReplayGateway())),
					new TestMutationQuarantineValueCodec(),
					Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));
			var record = repository.save(openRecord("aggregate.OrderDefinition"));

			var replayed = service.replay(record.quarantineId());

			assertThat(replayed.status())
					.as("status should be REPLAYED after successful gateway replay")
					.isEqualTo(MutationQuarantineStatus.REPLAYED);
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
					DefaultMutationQuarantineReplayRegistry.of(List.of(new FailingReplayGateway())),
					new TestMutationQuarantineValueCodec(),
					Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));
			var record = repository.save(openRecord("aggregate.OrderDefinition"));

			var replayed = service.replay(record.quarantineId());

			assertThat(replayed.status())
					.as("status should remain OPEN when gateway throws")
					.isEqualTo(MutationQuarantineStatus.OPEN);
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
					DefaultMutationQuarantineReplayRegistry.of(List.of()),
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