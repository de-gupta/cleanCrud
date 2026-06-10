package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.violation.MutationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.violation.MutationViolationKind;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.id.MutationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.infrastructure.persistence.model.MutationQuarantinePersistenceModel;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.port.persistence.MutationQuarantineRepository;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JpaMutationQuarantineStore implements MutationQuarantineRepository
{
	private final EntityManager entityManager;
	private final ObjectMapper objectMapper;
	private final JavaType violationValueType;

	public static JpaMutationQuarantineStore with(
			final EntityManager entityManager,
			final ObjectMapper objectMapper)
	{
		return new JpaMutationQuarantineStore(entityManager, objectMapper);
	}

	public JpaMutationQuarantineStore(
			final EntityManager entityManager,
			final ObjectMapper objectMapper)
	{
		this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.violationValueType = objectMapper.getTypeFactory()
		                                      .constructCollectionType(List.class, StoredViolation.class);
	}

	@Override
	@Transactional
	public MutationQuarantineRecord save(final MutationQuarantineRecord record)
	{
		entityManager.merge(toPersistenceModel(record));
		return record;
	}

	@Override
	@Transactional
	public MutationQuarantineRecord update(final MutationQuarantineRecord record)
	{
		entityManager.merge(toPersistenceModel(record));
		return record;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<MutationQuarantineRecord> findById(final MutationQuarantineId quarantineId)
	{
		return Optional.ofNullable(entityManager.find(
							   MutationQuarantinePersistenceModel.class,
							   quarantineId.value()))
		               .map(this::toDomainModel);
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<MutationQuarantineRecord> findOpen(final int limit)
	{
		if (limit < 1)
		{
			throw new IllegalArgumentException("limit");
		}
		return entityManager.createQuery(
									"""
											select quarantine
											from MutationQuarantinePersistenceModel quarantine
											where quarantine.status = :status
											order by quarantine.quarantinedAt
											""",
									MutationQuarantinePersistenceModel.class)
		                    .setParameter("status", MutationQuarantineStatus.OPEN)
		                    .setMaxResults(limit)
		                    .getResultList()
		                    .stream()
		                    .map(this::toDomainModel)
		                    .toList();
	}

	private MutationQuarantinePersistenceModel toPersistenceModel(final MutationQuarantineRecord record)
	{
		var persistenceModel = new MutationQuarantinePersistenceModel();
		persistenceModel.setQuarantineId(record.quarantineId().value());
		persistenceModel.setAggregateType(record.aggregateType());
		persistenceModel.setDomainIdType(record.domainIdType());
		persistenceModel.setDomainIdJson(record.domainIdJson());
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
		persistenceModel.setLastReplayOutcome(record.lastReplayOutcome().orElse(null));
		persistenceModel.setLastReplaySummary(record.lastReplaySummary().orElse(null));
		return persistenceModel;
	}

	private MutationQuarantineRecord toDomainModel(final MutationQuarantinePersistenceModel persistenceModel)
	{
		return new MutationQuarantineRecord(
				new MutationQuarantineId(persistenceModel.quarantineId()),
				persistenceModel.aggregateType(),
				persistenceModel.domainIdType(),
				persistenceModel.domainIdJson(),
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
				Optional.ofNullable(persistenceModel.lastReplayOutcome()),
				Optional.ofNullable(persistenceModel.lastReplaySummary()));
	}

	private String serializeViolations(final List<MutationPolicyViolation> violations)
	{
		try
		{
			var values = violations.stream()
			                       .map(StoredViolation::of)
			                       .toList();
			return objectMapper.writeValueAsString(values);
		}
		catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Failed to serialize mutation quarantine violations", e);
		}
	}

	private List<MutationPolicyViolation> deserializeViolations(final String violationsJson)
	{
		try
		{
			var storedViolations = (List<StoredViolation>) objectMapper.readValue(violationsJson, violationValueType);
			return storedViolations.stream()
			                       .map(StoredViolation::toDomain)
			                       .toList();
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Failed to deserialize mutation quarantine violations", e);
		}
	}

	private record StoredViolation(
			MutationViolationKind kind,
			String message,
			String invariantSeverity)
	{
		static StoredViolation of(final MutationPolicyViolation violation)
		{
			return new StoredViolation(
					violation.kind(),
					violation.message(),
					violation.severity().map(Enum::name).orElse(null));
		}

		MutationPolicyViolation toDomain()
		{
			return new MutationPolicyViolation(
					kind,
					message,
					Optional.ofNullable(invariantSeverity).map(InvariantSeverity::valueOf));
		}
	}
}