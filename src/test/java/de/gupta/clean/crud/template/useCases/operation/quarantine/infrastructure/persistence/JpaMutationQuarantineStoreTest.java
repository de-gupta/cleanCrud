package de.gupta.clean.crud.template.useCases.operation.quarantine.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayEnvelope;
import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayOutcome;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.violation.OperationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.violation.ViolationKind;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.*;
import de.gupta.clean.crud.template.useCases.operation.quarantine.infrastructure.persistence.model.MutationQuarantineEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaMutationQuarantineStoreTest.JpaEntityConfiguration.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class JpaMutationQuarantineStoreTest
{
	@jakarta.annotation.Resource
	private EntityManager entityManager;

	private JpaQuarantineStore<MutationReplayInputs> quarantineStore;

	@BeforeEach
	void setUp()
	{
		quarantineStore = JpaQuarantineStore.with(entityManager, new ObjectMapper(),
				MutationQuarantineEntity.class, MutationReplayInputs.class);
	}

	@Test
	void saveUpdateAndFindByIdRoundTripViolationsAndReplayMetadata()
	{
		var original = record("quarantine-1", Instant.parse("2026-06-09T10:00:00Z"));
		quarantineStore.save(original);
		var updated = original.replayAttempted(
				Instant.parse("2026-06-09T10:05:00Z"),
				QuarantineReplayOutcome.FAILED,
				Optional.of("still inconsistent"));
		quarantineStore.update(updated);
		entityManager.flush();
		entityManager.clear();

		var reloaded = quarantineStore.findById(original.quarantineId()).orElseThrow();

		assertThat(reloaded.status())
				.as("status should remain OPEN after failed replay attempt")
				.isEqualTo(QuarantineStatus.OPEN);
		assertThat(reloaded.replayAttemptCount())
				.as("replay attempt count should be 1 after one attempt")
				.isEqualTo(1);
		assertThat(reloaded.lastReplayOutcome())
				.as("last replay outcome should reflect FAILED result")
				.contains(QuarantineReplayOutcome.FAILED);
		assertThat(reloaded.violations().getFirst().severity())
				.as("reloaded violations should round-trip the invariant severity")
				.contains(InvariantSeverity.HARD);
	}

	@Test
	void findOpenReturnsOnlyOpenRecordsInInsertionOrder()
	{
		quarantineStore.save(record("open-1", Instant.parse("2026-06-09T10:00:00Z")));
		quarantineStore.save(record("open-2", Instant.parse("2026-06-09T10:01:00Z")));
		quarantineStore.save(record("dismissed", Instant.parse("2026-06-09T10:02:00Z"))
				.dismissed(Instant.parse("2026-06-09T10:03:00Z")));
		entityManager.flush();
		entityManager.clear();

		assertThat(quarantineStore.findOpen(10))
				.as("findOpen should return only open records in quarantinedAt order")
				.extracting(QuarantineRecord::quarantineId)
				.containsExactly(new QuarantineId("open-1"), new QuarantineId("open-2"));
	}

	private QuarantineRecord<MutationReplayInputs> record(final String id, final Instant quarantinedAt)
	{
		return new QuarantineRecord<>(
				new QuarantineId(id),
				"aggregate.OrderDefinition",
				new OperationInvocationMetadata(
						OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
						OperationFamily.APPLICATION,
						Optional.empty(),
						Optional.empty()),
				new MutationReplayInputs(
						QuarantineReplayEnvelope.of(String.class.getName(), "\"order-1\""),
						QuarantineReplayEnvelope.of("payload.Type", "{\"command\":\"ack\"}")),
				List.of(new OperationPolicyViolation(
						ViolationKind.INVARIANT,
						"hard violation",
						Optional.of(InvariantSeverity.HARD))),
				QuarantineStatus.OPEN,
				quarantinedAt,
				quarantinedAt,
				0,
				Optional.empty(),
				Optional.empty(),
				Optional.empty());
	}

	@org.springframework.boot.SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = MutationQuarantineEntity.class)
	static class JpaEntityConfiguration
	{
	}
}