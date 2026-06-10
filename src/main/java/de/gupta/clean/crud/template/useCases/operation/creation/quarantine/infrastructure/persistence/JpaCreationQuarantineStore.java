package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.violation.CreationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.violation.CreationViolationKind;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.infrastructure.persistence.model.CreationQuarantinePersistenceModel;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.port.persistence.CreationQuarantineRepository;
import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayOutcome;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.invariant.InvariantViolation;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JpaCreationQuarantineStore implements CreationQuarantineRepository
{
	private final EntityManager entityManager;
	private final ObjectMapper objectMapper;
	private final JavaType violationValueType;

	public static JpaCreationQuarantineStore with(
			final EntityManager entityManager,
			final ObjectMapper objectMapper)
	{
		return new JpaCreationQuarantineStore(entityManager, objectMapper);
	}

	@Override
	@Transactional
	public CreationQuarantineRecord save(final CreationQuarantineRecord record)
	{
		entityManager.merge(toPersistenceModel(record));
		return record;
	}

	@Override
	@Transactional
	public CreationQuarantineRecord update(final CreationQuarantineRecord record)
	{
		entityManager.merge(toPersistenceModel(record));
		return record;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<CreationQuarantineRecord> findById(final CreationQuarantineId quarantineId)
	{
		return Optional.ofNullable(entityManager.find(
							   CreationQuarantinePersistenceModel.class,
							   quarantineId.value()))
		               .map(this::toDomainModel);
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<CreationQuarantineRecord> findOpen(final int limit)
	{
		if (limit < 1)
		{
			throw new IllegalArgumentException("limit");
		}
		return entityManager.createQuery(
									"""
											select quarantine
											from CreationQuarantinePersistenceModel quarantine
											where quarantine.status = :status
											order by quarantine.quarantinedAt
											""",
									CreationQuarantinePersistenceModel.class)
		                    .setParameter("status", CreationQuarantineStatus.OPEN)
		                    .setMaxResults(limit)
		                    .getResultList()
		                    .stream()
		                    .map(this::toDomainModel)
		                    .toList();
	}

	private String serializeViolations(final List<CreationPolicyViolation> violations)
	{
		try
		{
			var values = violations.stream()
			                       .map(StoredViolation::of)
			                       .toList();
			return objectMapper.writeValueAsString(values);
		}
		catch (JsonProcessingException caught)
		{
			throw new IllegalStateException("Failed to serialize creation quarantine violations", caught);
		}
	}

	private CreationQuarantinePersistenceModel toPersistenceModel(final CreationQuarantineRecord record)
	{
		var persistenceModel = new CreationQuarantinePersistenceModel();
		persistenceModel.setQuarantineId(record.quarantineId().value());
		persistenceModel.setAggregateType(record.aggregateType());
		persistenceModel.setPayloadType(record.payloadType());
		persistenceModel.setPayloadJson(record.payloadJson());
		persistenceModel.setSource(record.source());
		persistenceModel.setFamily(record.family());
		persistenceModel.setCorrelationId(record.correlationId().map(OperationCorrelationId::value).orElse(null));
		persistenceModel.setCausationId(record.causationId().map(OperationCausationId::value).orElse(null));
		persistenceModel.setStatus(record.status());
		persistenceModel.setViolationsJson(serializeViolations(record.violations()));
		persistenceModel.setQuarantinedAt(record.quarantinedAt());
		persistenceModel.setUpdatedAt(record.updatedAt());
		persistenceModel.setReplayAttemptCount(record.replayAttemptCount());
		persistenceModel.setLastReplayAt(record.lastReplayAt().orElse(null));
		persistenceModel.setLastReplayOutcome(record.lastReplayOutcome().map(Enum::name).orElse(null));
		persistenceModel.setLastReplaySummary(record.lastReplaySummary().orElse(null));
		return persistenceModel;
	}

	private CreationQuarantineRecord toDomainModel(final CreationQuarantinePersistenceModel persistenceModel)
	{
		return new CreationQuarantineRecord(
				new CreationQuarantineId(persistenceModel.quarantineId()),
				persistenceModel.aggregateType(),
				persistenceModel.payloadType(),
				persistenceModel.payloadJson(),
				persistenceModel.source(),
				persistenceModel.family(),
				Optional.ofNullable(persistenceModel.correlationId()).map(OperationCorrelationId::new),
				Optional.ofNullable(persistenceModel.causationId()).map(OperationCausationId::new),
				persistenceModel.status(),
				deserializeViolations(persistenceModel.violationsJson()),
				persistenceModel.quarantinedAt(),
				persistenceModel.updatedAt(),
				persistenceModel.replayAttemptCount(),
				Optional.ofNullable(persistenceModel.lastReplayAt()),
				Optional.ofNullable(persistenceModel.lastReplayOutcome()).map(QuarantineReplayOutcome::valueOf),
				Optional.ofNullable(persistenceModel.lastReplaySummary()));
	}

	private List<CreationPolicyViolation> deserializeViolations(final String violationsJson)
	{
		try
		{
			var storedViolations = (List<StoredViolation>) objectMapper.readValue(violationsJson, violationValueType);
			return storedViolations.stream()
			                       .map(StoredViolation::toDomain)
			                       .toList();
		}
		catch (IOException caught)
		{
			throw new IllegalStateException("Failed to deserialize creation quarantine violations", caught);
		}
	}

	JpaCreationQuarantineStore(
			final EntityManager entityManager,
			final ObjectMapper objectMapper)
	{
		this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.violationValueType = objectMapper.getTypeFactory()
		                                      .constructCollectionType(List.class, StoredViolation.class);
	}

	private record StoredViolation(
			CreationViolationKind kind,
			String message,
			String invariantSeverity)
	{
		static StoredViolation of(final CreationPolicyViolation violation)
		{
			return new StoredViolation(
					violation.kind(),
					violation.message(),
					violation.invariantViolation().map(invariant -> invariant.severity().name()).orElse(null));
		}

		CreationPolicyViolation toDomain()
		{
			return new CreationPolicyViolation(
					kind,
					message,
					Optional.ofNullable(invariantSeverity)
					        .map(InvariantSeverity::valueOf)
					        .map(severity -> new InvariantViolation(message, severity)));
		}
	}
}