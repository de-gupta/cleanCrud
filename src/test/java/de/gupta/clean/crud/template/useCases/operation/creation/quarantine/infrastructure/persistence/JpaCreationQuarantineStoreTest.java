package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.violation.CreationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.infrastructure.persistence.model.CreationQuarantinePersistenceModel;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.invariant.InvariantViolation;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaCreationQuarantineStoreTest.JpaEntityConfiguration.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class JpaCreationQuarantineStoreTest
{
	@jakarta.annotation.Resource
	private EntityManager entityManager;

	private JpaCreationQuarantineStore quarantineStore;

	@BeforeEach
	void setUp()
	{
		quarantineStore = JpaCreationQuarantineStore.with(entityManager, new ObjectMapper());
	}

	@Test
	void saveUpdateAndFindByIdRoundTripViolationsAndReplayMetadata()
	{
		var original = record("quarantine-1", Instant.parse("2026-06-09T10:00:00Z"));
		quarantineStore.save(original);
		var updated = original.replayAttempted(
				Instant.parse("2026-06-09T10:05:00Z"),
				"FAILED",
				Optional.of("still inconsistent"));
		quarantineStore.update(updated);
		entityManager.flush();
		entityManager.clear();

		var reloaded = quarantineStore.findById(original.quarantineId()).orElseThrow();

		assertThat(reloaded.status())
				.as("status should remain OPEN after failed replay attempt")
				.isEqualTo(CreationQuarantineStatus.OPEN);
		assertThat(reloaded.replayAttemptCount())
				.as("replay attempt count should be 1 after one attempt")
				.isEqualTo(1);
		assertThat(reloaded.lastReplayOutcome())
				.as("last replay outcome should reflect FAILED result")
				.contains("FAILED");
		assertThat(reloaded.violations().getFirst().invariantViolation())
				.as("reloaded violations should round-trip the invariant violation")
				.contains(InvariantViolation.hard("hard violation"));
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
				.extracting(CreationQuarantineRecord::quarantineId)
				.containsExactly(new CreationQuarantineId("open-1"), new CreationQuarantineId("open-2"));
	}

	private CreationQuarantineRecord record(final String id, final Instant quarantinedAt)
	{
		return new CreationQuarantineRecord(
				new CreationQuarantineId(id),
				"aggregate.OrderDefinition",
				"payload.Type",
				"{\"command\":\"register\"}",
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
				OperationFamily.APPLICATION,
				Optional.empty(),
				Optional.empty(),
				CreationQuarantineStatus.OPEN,
				java.util.List.of(CreationPolicyViolation.invariant(InvariantViolation.hard("hard violation"))),
				quarantinedAt,
				quarantinedAt,
				0,
				Optional.empty(),
				Optional.empty(),
				Optional.empty());
	}

	@org.springframework.boot.SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = CreationQuarantinePersistenceModel.class)
	static class JpaEntityConfiguration
	{
	}
}
