package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.TestOperationPayload;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultCreationQuarantineServiceTest
{
	@Test
	void recordAssignsPersistentQuarantineId()
	{
		var service = DefaultCreationQuarantineService.with(
				new InMemoryCreationQuarantineRepository(),
				DefaultCreationQuarantineReplayRegistry.of(java.util.List.of()),
				new ObjectMapper(),
				Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));

		var persisted = service.record(new CreationQuarantineSubmission(
				"aggregate.OrderDefinition",
				new CreationRequest<>(new TestOperationPayload("ack"),
						OperationSource.AUTHORITATIVE_EXTERNAL_EVENT),
				new CreationQuarantineRequest(
						OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
						java.util.List.of(CreationPolicyViolation.externalConsistency("broker mismatch")))));

		assertThat(persisted.quarantineId()).isPresent();
	}

	@Test
	void replayMarksRecordReplayedWhenGatewayAppliesCreation()
	{
		var repository = new InMemoryCreationQuarantineRepository();
		var service = DefaultCreationQuarantineService.with(
				repository,
				DefaultCreationQuarantineReplayRegistry.of(java.util.List.of(new SuccessfulReplayGateway())),
				new ObjectMapper(),
				Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));
		var record = repository.save(record("aggregate.OrderDefinition"));

		var replayed = service.replay(record.quarantineId());

		assertThat(replayed.status()).isEqualTo(CreationQuarantineStatus.REPLAYED);
		assertThat(replayed.replayAttemptCount()).isEqualTo(1);
	}

	@Test
	void replayLeavesRecordOpenWhenGatewayRejects()
	{
		var repository = new InMemoryCreationQuarantineRepository();
		var service = DefaultCreationQuarantineService.with(
				repository,
				DefaultCreationQuarantineReplayRegistry.of(java.util.List.of(new FailingReplayGateway())),
				new ObjectMapper(),
				Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));
		var record = repository.save(record("aggregate.OrderDefinition"));

		var replayed = service.replay(record.quarantineId());

		assertThat(replayed.status()).isEqualTo(CreationQuarantineStatus.OPEN);
		assertThat(replayed.lastReplayOutcome()).contains("FAILED");
	}

	@Test
	void dismissRejectsAlreadyResolvedRecords()
	{
		var repository = new InMemoryCreationQuarantineRepository();
		var service = DefaultCreationQuarantineService.with(
				repository,
				DefaultCreationQuarantineReplayRegistry.of(java.util.List.of()),
				new ObjectMapper(),
				Clock.fixed(Instant.parse("2026-06-09T10:15:30Z"), ZoneOffset.UTC));
		var dismissed = repository.save(record("aggregate.OrderDefinition")
				.dismissed(Instant.parse("2026-06-09T10:16:00Z")));

		assertThrows(RuntimeException.class, () -> service.dismiss(dismissed.quarantineId()));
	}

	private CreationQuarantineRecord record(final String aggregateType)
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
				java.util.List.of(CreationPolicyViolation.externalConsistency("broker mismatch")),
				Instant.parse("2026-06-09T10:15:00Z"),
				Instant.parse("2026-06-09T10:15:00Z"),
				0,
				Optional.empty(),
				Optional.empty(),
				Optional.empty());
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
}
