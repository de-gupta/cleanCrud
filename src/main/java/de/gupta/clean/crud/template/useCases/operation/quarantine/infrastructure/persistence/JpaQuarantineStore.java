package de.gupta.clean.crud.template.useCases.operation.quarantine.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayOutcome;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.violation.OperationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.violation.ViolationKind;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.*;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.port.QuarantineRepository;
import de.gupta.clean.crud.template.useCases.operation.quarantine.infrastructure.persistence.model.QuarantinePersistenceModel;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JpaQuarantineStore<P extends PayloadReplayInputs> implements QuarantineRepository<P>
{
	private final EntityManager entityManager;
	private final ObjectMapper objectMapper;
	private final Class<? extends QuarantinePersistenceModel> entityClass;
	private final Class<P> replayInputsType;
	private final JavaType violationValueType;

	public static <P extends PayloadReplayInputs> JpaQuarantineStore<P> with(
			final EntityManager entityManager,
			final ObjectMapper objectMapper,
			final Class<? extends QuarantinePersistenceModel> entityClass,
			final Class<P> replayInputsType)
	{
		return new JpaQuarantineStore<>(entityManager, objectMapper, entityClass, replayInputsType);
	}

	@Override
	@Transactional
	public QuarantineRecord<P> save(final QuarantineRecord<P> record)
	{
		entityManager.merge(toPersistenceModel(record));
		return record;
	}

	@Override
	@Transactional
	public QuarantineRecord<P> update(final QuarantineRecord<P> record)
	{
		entityManager.merge(toPersistenceModel(record));
		return record;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<QuarantineRecord<P>> findById(final QuarantineId quarantineId)
	{
		return Optional.ofNullable(entityManager.find(entityClass, quarantineId.value())).map(this::toDomainModel);
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<QuarantineRecord<P>> findOpen(final int limit)
	{
		if (limit < 1)
		{
			throw new IllegalArgumentException("limit");
		}
		return entityManager.createQuery(
									"select q from " + entityClass.getSimpleName() + " q where q.status = :status order by q.quarantinedAt",
									entityClass).setParameter("status", QuarantineStatus.OPEN).setMaxResults(limit).getResultList().stream()
		                    .map(this::toDomainModel).toList();
	}

	private QuarantinePersistenceModel toPersistenceModel(final QuarantineRecord<P> record)
	{
		QuarantinePersistenceModel model;
		try
		{
			model = entityClass.getDeclaredConstructor().newInstance();
		}
		catch (Exception caught)
		{
			throw new IllegalStateException("Failed to instantiate " + entityClass.getSimpleName(), caught);
		}
		model.setQuarantineId(record.quarantineId().value());
		model.setAggregateKey(record.aggregateKey());
		model.setSource(record.metadata().source());
		model.setFamily(record.metadata().family());
		model.setCorrelationId(record.metadata().correlationId().map(OperationCorrelationId::value).orElse(null));
		model.setCausationId(record.metadata().causationId().map(OperationCausationId::value).orElse(null));
		model.setStatus(record.status());
		model.setReplayInputsJson(serializeReplayInputs(record.replayInputs()));
		model.setViolationsJson(serializeViolations(record.violations()));
		model.setQuarantinedAt(record.quarantinedAt());
		model.setUpdatedAt(record.updatedAt());
		model.setReplayAttemptCount(record.replayAttemptCount());
		model.setLastReplayAt(record.lastReplayAt().orElse(null));
		model.setLastReplayOutcome(record.lastReplayOutcome().map(Enum::name).orElse(null));
		model.setLastReplaySummary(record.lastReplaySummary().orElse(null));
		return model;
	}

	private QuarantineRecord<P> toDomainModel(final QuarantinePersistenceModel model)
	{
		return new QuarantineRecord<>(new QuarantineId(model.quarantineId()), model.aggregateKey(),
				new OperationInvocationMetadata(model.source(), model.family(),
						Optional.ofNullable(model.correlationId()).map(OperationCorrelationId::new),
						Optional.ofNullable(model.causationId()).map(OperationCausationId::new)),
				deserializeReplayInputs(model.replayInputsJson()), deserializeViolations(model.violationsJson()),
				model.status(), model.quarantinedAt(), model.updatedAt(), model.replayAttemptCount(),
				Optional.ofNullable(model.lastReplayAt()),
				Optional.ofNullable(model.lastReplayOutcome()).map(QuarantineReplayOutcome::valueOf),
				Optional.ofNullable(model.lastReplaySummary()));
	}

	private String serializeReplayInputs(final P replayInputs)
	{
		try
		{
			return objectMapper.writeValueAsString(replayInputs);
		}
		catch (JsonProcessingException caught)
		{
			throw new IllegalStateException("Failed to serialize quarantine replay inputs", caught);
		}
	}

	private P deserializeReplayInputs(final String json)
	{
		try
		{
			return objectMapper.readValue(json, replayInputsType);
		}
		catch (IOException caught)
		{
			throw new IllegalStateException("Failed to deserialize quarantine replay inputs", caught);
		}
	}

	private String serializeViolations(final List<OperationPolicyViolation> violations)
	{
		try
		{
			return objectMapper.writeValueAsString(violations.stream().map(StoredViolation::of).toList());
		}
		catch (JsonProcessingException caught)
		{
			throw new IllegalStateException("Failed to serialize quarantine violations", caught);
		}
	}

	private List<OperationPolicyViolation> deserializeViolations(final String json)
	{
		try
		{
			var stored = (List<StoredViolation>) objectMapper.readValue(json, violationValueType);
			return stored.stream().map(StoredViolation::toDomain).toList();
		}
		catch (IOException caught)
		{
			throw new IllegalStateException("Failed to deserialize quarantine violations", caught);
		}
	}

	JpaQuarantineStore(final EntityManager entityManager, final ObjectMapper objectMapper,
	                   final Class<? extends QuarantinePersistenceModel> entityClass, final Class<P> replayInputsType)
	{
		this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
		this.replayInputsType = Objects.requireNonNull(replayInputsType, "replayInputsType");
		this.violationValueType =
				objectMapper.getTypeFactory().constructCollectionType(List.class, StoredViolation.class);
	}

	private record StoredViolation(ViolationKind kind, String message, String invariantSeverity)
	{
		static StoredViolation of(final OperationPolicyViolation violation)
		{
			return new StoredViolation(violation.kind(), violation.message(),
					violation.severity().map(Enum::name).orElse(null));
		}

		OperationPolicyViolation toDomain()
		{
			return new OperationPolicyViolation(kind, message,
					Optional.ofNullable(invariantSeverity).map(InvariantSeverity::valueOf));
		}
	}
}