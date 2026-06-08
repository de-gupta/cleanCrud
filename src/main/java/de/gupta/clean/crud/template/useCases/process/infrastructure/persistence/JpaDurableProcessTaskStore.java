package de.gupta.clean.crud.template.useCases.process.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.CorrelationId;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.BackoffPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.RetryPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask;
import de.gupta.clean.crud.template.useCases.process.infrastructure.persistence.model.DurableProcessTaskPersistenceModel;
import de.gupta.clean.crud.template.useCases.process.port.persistence.DurableProcessTaskRepository;
import de.gupta.clean.crud.template.useCases.process.port.scheduling.DurableProcessTaskScheduler;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class JpaDurableProcessTaskStore
		implements DurableProcessTaskRepository, DurableProcessTaskScheduler
{
	private final EntityManager entityManager;
	private final ObjectMapper objectMapper;

	public static JpaDurableProcessTaskStore with(
			final EntityManager entityManager,
			final ObjectMapper objectMapper)
	{
		return new JpaDurableProcessTaskStore(entityManager, objectMapper);
	}

	@Override
	@Transactional
	public DurableProcessTask save(final DurableProcessTask task)
	{
		var persistenceModel = toPersistenceModel(task);
		entityManager.merge(persistenceModel);
		return task;
	}

	@Override
	@Transactional
	public DurableProcessTask update(final DurableProcessTask task)
	{
		var persistenceModel = toPersistenceModel(task);
		entityManager.merge(persistenceModel);
		return task;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DurableProcessTask> findById(final DurableProcessTaskId taskId)
	{
		return Optional.ofNullable(entityManager.find(DurableProcessTaskPersistenceModel.class, taskId.value()))
		               .map(this::toDomainModel);
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<DurableProcessTask> findDueTasks(final Instant asOf, final int limit)
	{
		Objects.requireNonNull(asOf, "asOf");
		if (limit < 1)
		{
			throw new IllegalArgumentException("limit");
		}
		return entityManager.createQuery(
									"""
											select task
											from DurableProcessTaskPersistenceModel task
											where task.status in :readyStatuses
											  and (task.nextAttemptAt is null or task.nextAttemptAt <= :asOf)
											order by coalesce(task.nextAttemptAt, task.createdAt), task.createdAt
											""",
									DurableProcessTaskPersistenceModel.class)
		                    .setParameter("readyStatuses",
									java.util.List.of(
											de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTaskStatus.PENDING,
											de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTaskStatus.WAITING_RETRY))
		                    .setParameter("asOf", asOf)
		                    .setMaxResults(limit)
		                    .getResultList()
		                    .stream()
		                    .map(this::toDomainModel)
		                    .toList();
	}

	@Override
	@Transactional
	public DurableProcessTask scheduleRetry(final DurableProcessTask task, final Instant nextAttemptAt)
	{
		entityManager.merge(toPersistenceModel(task));
		return task;
	}

	private DurableProcessTaskPersistenceModel toPersistenceModel(final DurableProcessTask task)
	{
		var persistenceModel = new DurableProcessTaskPersistenceModel();
		persistenceModel.setTaskId(task.taskId().value());
		persistenceModel.setProcessType(task.processType());
		persistenceModel.setCorrelationId(task.correlationId().value());
		persistenceModel.setPayloadType(task.payload().getClass().getName());
		persistenceModel.setPayloadJson(serialize(task.payload()));
		persistenceModel.setRetryMaxAttempts(task.retryPolicy().maxAttempts());
		persistenceModel.setRetryInitialDelayMillis(task.retryPolicy().backoffPolicy().initialDelay().toMillis());
		persistenceModel.setRetryMultiplier(task.retryPolicy().backoffPolicy().multiplier());
		persistenceModel.setRetryMaxDelayMillis(task.retryPolicy()
		                                            .backoffPolicy()
		                                            .maxDelay()
		                                            .map(Duration::toMillis)
		                                            .orElse(null));
		persistenceModel.setStatus(task.status());
		persistenceModel.setAttemptCount(task.attemptCount());
		persistenceModel.setNextAttemptAt(task.nextAttemptAt().orElse(null));
		persistenceModel.setCreatedAt(task.createdAt());
		persistenceModel.setUpdatedAt(task.updatedAt());
		persistenceModel.setLastFailureSummary(task.lastFailureSummary().orElse(null));
		persistenceModel.setLastOutcomeCode(task.lastOutcomeCode().orElse(null));
		return persistenceModel;
	}

	private DurableProcessTask toDomainModel(final DurableProcessTaskPersistenceModel persistenceModel)
	{
		return new DurableProcessTask(
				new DurableProcessTaskId(persistenceModel.taskId()),
				persistenceModel.processType(),
				new CorrelationId(persistenceModel.correlationId()),
				deserializePayload(persistenceModel.payloadType(), persistenceModel.payloadJson()),
				new RetryPolicy(
						persistenceModel.retryMaxAttempts(),
						new BackoffPolicy(
								Duration.ofMillis(persistenceModel.retryInitialDelayMillis()),
								persistenceModel.retryMultiplier(),
								Optional.ofNullable(persistenceModel.retryMaxDelayMillis()).map(Duration::ofMillis))),
				persistenceModel.status(),
				persistenceModel.attemptCount(),
				Optional.ofNullable(persistenceModel.nextAttemptAt()),
				persistenceModel.createdAt(),
				persistenceModel.updatedAt(),
				Optional.ofNullable(persistenceModel.lastFailureSummary()),
				Optional.ofNullable(persistenceModel.lastOutcomeCode()));
	}

	private String serialize(final DurableProcessPayload payload)
	{
		try
		{
			return objectMapper.writeValueAsString(payload);
		}
		catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Failed to serialize durable process payload", e);
		}
	}

	private DurableProcessPayload deserializePayload(final String payloadType, final String payloadJson)
	{
		try
		{
			var payloadClass = Class.forName(payloadType);
			return (DurableProcessPayload) objectMapper.readValue(payloadJson, payloadClass);
		}
		catch (ClassNotFoundException | IOException e)
		{
			throw new IllegalStateException("Failed to deserialize durable process payload", e);
		}
	}

	private JpaDurableProcessTaskStore(
			final EntityManager entityManager,
			final ObjectMapper objectMapper)
	{
		this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
	}
}