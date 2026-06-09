package de.gupta.clean.crud.template.useCases.mutation.quarantine.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationFamily;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation.MutationPolicyViolation;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.MutationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.id.MutationQuarantineId;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.infrastructure.persistence.model.MutationQuarantinePersistenceModel;
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
@Import(JpaMutationQuarantineStoreTest.JpaEntityConfiguration.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class JpaMutationQuarantineStoreTest
{
	@jakarta.annotation.Resource
	private EntityManager entityManager;

	private JpaMutationQuarantineStore quarantineStore;

	@BeforeEach
	void setUp()
	{
		quarantineStore = JpaMutationQuarantineStore.with(entityManager, new ObjectMapper());
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

		assertThat(reloaded.status()).isEqualTo(MutationQuarantineStatus.OPEN);
		assertThat(reloaded.replayAttemptCount()).isEqualTo(1);
		assertThat(reloaded.lastReplayOutcome()).contains("FAILED");
		assertThat(reloaded.violations().getFirst().severity()).contains(InvariantSeverity.HARD);
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
				.extracting(MutationQuarantineRecord::quarantineId)
				.containsExactly(new MutationQuarantineId("open-1"), new MutationQuarantineId("open-2"));
	}

	private MutationQuarantineRecord record(final String id, final Instant quarantinedAt)
	{
		return new MutationQuarantineRecord(
				new MutationQuarantineId(id),
				"aggregate.OrderDefinition",
				String.class.getName(),
				"\"order-1\"",
				"payload.Type",
				"{\"command\":\"ack\"}",
				MutationSource.AUTHORITATIVE_EXTERNAL_EVENT,
				MutationFamily.APPLICATION,
				Optional.empty(),
				Optional.empty(),
				MutationQuarantineStatus.OPEN,
				java.util.List.of(new MutationPolicyViolation(
						de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation.MutationViolationKind.INVARIANT,
						"hard violation",
						Optional.of(InvariantSeverity.HARD))),
				quarantinedAt,
				quarantinedAt,
				0,
				Optional.empty(),
				Optional.empty(),
				Optional.empty());
	}

	@org.springframework.boot.SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = MutationQuarantinePersistenceModel.class)
	static class JpaEntityConfiguration
	{
	}
}