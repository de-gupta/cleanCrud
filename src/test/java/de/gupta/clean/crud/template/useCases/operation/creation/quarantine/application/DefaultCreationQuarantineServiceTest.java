package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreateResult;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationContext;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationResult;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.evaluation.CreationPolicyDecision;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine.CreationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.violation.CreationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.recording.CreationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.port.persistence.CreationQuarantineRepository;
import de.gupta.clean.crud.template.useCases.operation.domain.model.*;
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
	private CreationQuarantineRecord openRecord(final String aggregateType)
	{
		return new CreationQuarantineRecord(
				new CreationQuarantineId("quarantine-1"),
				aggregateType,
				TestOperationPayload.class.getName(),
				"{\"value\":\"ack\"}",
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
				OperationFamily.APPLICATION,
				Optional.empty(),
				Optional.empty(),
				CreationQuarantineStatus.OPEN,
				List.of(CreationPolicyViolation.externalConsistency("broker mismatch")),
				Instant.parse("2026-06-09T10:15:00Z"),
				Instant.parse("2026-06-09T10:15:00Z"),
				0,
				Optional.empty(),
				Optional.empty(),
				Optional.empty());
	}

	private static final class TestCreationQuarantinePayloadCodec implements CreationQuarantinePayloadCodec
	{
		@Override
		public SerializedCreationPayload serialize(final ApplicationOperationPayload payload)
		{
			var testPayload = (TestOperationPayload) payload;
			return new SerializedCreationPayload(TestOperationPayload.class.getName(), testPayload.value());
		}

		@Override
		public ApplicationOperationPayload deserialize(final SerializedCreationPayload payload)
		{
			return new TestOperationPayload(payload.payloadJson());
		}
	}

	private static final class InMemoryCreationQuarantineRepository implements CreationQuarantineRepository
	{
		private final Map<CreationQuarantineId, CreationQuarantineRecord> records = new LinkedHashMap<>();

		@Override
		public CreationQuarantineRecord save(final CreationQuarantineRecord record)
		{
			records.put(record.quarantineId(), record);
			return record;
		}

		@Override
		public CreationQuarantineRecord update(final CreationQuarantineRecord record)
		{
			records.put(record.quarantineId(), record);
			return record;
		}

		@Override
		public Optional<CreationQuarantineRecord> findById(final CreationQuarantineId quarantineId)
		{
			return Optional.ofNullable(records.get(quarantineId));
		}

		@Override
		public Collection<CreationQuarantineRecord> findOpen(final int limit)
		{
			return records.values().stream().filter(CreationQuarantineRecord::open).limit(limit).toList();
		}
	}

	private static final class SuccessfulReplayGateway implements CreationQuarantineReplayGateway
	{
		@Override
		public String aggregateType()
		{
			return "aggregate.OrderDefinition";
		}

		@Override
		public CreationResult<?, ?> replay(final CreationQuarantineReplayCommand command)
		{
			return CreationResult.created(
					new CreationContext<>(
							Optional.of("order-1"),
							OperationSource.ADMINISTRATIVE_REPLAY,
							command.family(),
							command.payload().getClass(),
							command.correlationId(),
							command.causationId(),
							Optional.empty(),
							Optional.of("CREATED")),
					CreationPolicyDecision.allow(),
					CreateResult.of("order-1", "CREATED"));
		}
	}

	private static final class FailingReplayGateway implements CreationQuarantineReplayGateway
	{
		@Override
		public String aggregateType()
		{
			return "aggregate.OrderDefinition";
		}

		@Override
		public CreationResult<?, ?> replay(final CreationQuarantineReplayCommand command)
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
					DefaultCreationQuarantineReplayRegistry.of(List.of()),
					new TestCreationQuarantinePayloadCodec(),
					Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));

			var persisted = service.record(new CreationQuarantineSubmission(
					"aggregate.OrderDefinition",
					new CreationRequest<>(new TestOperationPayload("ack"),
							OperationSource.AUTHORITATIVE_EXTERNAL_EVENT),
					new CreationQuarantineRequest(
							OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
							List.of(CreationPolicyViolation.externalConsistency("broker mismatch")))));

			assertThat(persisted.quarantineId())
					.as("recorded submission should carry a persisted quarantine id")
					.isPresent();
		}
	}

	@Nested
	class WhenReplaying
	{
		@Test
		void marksRecordReplayedWhenGatewayAppliesCreation()
		{
			var repository = new InMemoryCreationQuarantineRepository();
			var service = DefaultCreationQuarantineService.with(
					repository,
					DefaultCreationQuarantineReplayRegistry.of(List.of(new SuccessfulReplayGateway())),
					new TestCreationQuarantinePayloadCodec(),
					Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));
			var record = repository.save(openRecord("aggregate.OrderDefinition"));

			var replayed = service.replay(record.quarantineId());

			assertThat(replayed.status())
					.as("status should be REPLAYED after successful gateway replay")
					.isEqualTo(CreationQuarantineStatus.REPLAYED);
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
					DefaultCreationQuarantineReplayRegistry.of(List.of(new FailingReplayGateway())),
					new TestCreationQuarantinePayloadCodec(),
					Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));
			var record = repository.save(openRecord("aggregate.OrderDefinition"));

			var replayed = service.replay(record.quarantineId());

			assertThat(replayed.status())
					.as("status should remain OPEN when gateway throws")
					.isEqualTo(CreationQuarantineStatus.OPEN);
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
					DefaultCreationQuarantineReplayRegistry.of(List.of()),
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