package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application;

import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultMutationQuarantineServiceTest
{
	@org.junit.jupiter.api.Test
	void recordAssignsPersistentQuarantineId()
	{
		var service = DefaultMutationQuarantineService.with(
				new InMemoryMutationQuarantineRepository(),
				DefaultMutationQuarantineReplayRegistry.of(java.util.List.of()),
				new TestMutationQuarantineValueCodec(),
				Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));

		var persisted = service.record(new MutationQuarantineSubmission(
				"aggregate.OrderDefinition",
				new MutationRequest<>("order-1", new AcknowledgeOrder("ack"),
						OperationSource.AUTHORITATIVE_EXTERNAL_EVENT),
				new MutationQuarantineRequest(
						OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
						java.util.List.of(MutationPolicyViolation.externalConsistency("broker mismatch")))));

		assertThat(persisted.quarantineId()).isPresent();
	}

	@org.junit.jupiter.api.Test
	void replayMarksRecordReplayedWhenGatewayAppliesMutation()
	{
		var repository = new InMemoryMutationQuarantineRepository();
		var service = DefaultMutationQuarantineService.with(
				repository,
				DefaultMutationQuarantineReplayRegistry.of(java.util.List.of(new SuccessfulReplayGateway())),
				new TestMutationQuarantineValueCodec(),
				Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));
		var record = repository.save(record("aggregate.OrderDefinition"));

		var replayed = service.replay(record.quarantineId());

		assertThat(replayed.status()).isEqualTo(MutationQuarantineStatus.REPLAYED);
		assertThat(replayed.replayAttemptCount()).isEqualTo(1);
	}

	@org.junit.jupiter.api.Test
	void replayLeavesRecordOpenWhenGatewayRejects()
	{
		var repository = new InMemoryMutationQuarantineRepository();
		var service = DefaultMutationQuarantineService.with(
				repository,
				DefaultMutationQuarantineReplayRegistry.of(java.util.List.of(new FailingReplayGateway())),
				new TestMutationQuarantineValueCodec(),
				Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));
		var record = repository.save(record("aggregate.OrderDefinition"));

		var replayed = service.replay(record.quarantineId());

		assertThat(replayed.status()).isEqualTo(MutationQuarantineStatus.OPEN);
		assertThat(replayed.lastReplayOutcome()).contains("FAILED");
	}

	@org.junit.jupiter.api.Test
	void dismissRejectsAlreadyResolvedRecords()
	{
		var repository = new InMemoryMutationQuarantineRepository();
		var service = DefaultMutationQuarantineService.with(
				repository,
				DefaultMutationQuarantineReplayRegistry.of(java.util.List.of()),
				new TestMutationQuarantineValueCodec(),
				Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));
		var dismissed = repository.save(record("aggregate.OrderDefinition")
				.dismissed(Instant.parse("2026-06-09T10:16:00Z")));

		assertThrows(RuntimeException.class, () -> service.dismiss(dismissed.quarantineId()));
	}

	private MutationQuarantineRecord record(final String aggregateType)
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
				java.util.List.of(MutationPolicyViolation.externalConsistency("broker mismatch")),
				Instant.parse("2026-06-09T10:15:00Z"),
				Instant.parse("2026-06-09T10:15:00Z"),
				0,
				Optional.empty(),
				Optional.empty(),
				Optional.empty());
	}

	public record AcknowledgeOrder(String value) implements ApplicationOperationPayload
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
}