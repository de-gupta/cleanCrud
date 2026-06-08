package de.gupta.clean.crud.template.useCases.process.application.registration;

import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessDefinition;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessTrigger;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.CorrelationId;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.BackoffPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.RetryPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTaskStatus;
import de.gupta.clean.crud.template.useCases.process.port.persistence.DurableProcessTaskRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDurableProcessStarterTest
{
	@Test
	void starterPersistsPendingTaskWithRetryPolicyAndCorrelation()
	{
		var repository = new RecordingRepository();
		var starter = DefaultDurableProcessStarter.with(
				repository,
				Clock.fixed(Instant.parse("2026-06-08T10:00:00Z"), ZoneOffset.UTC));
		var retryPolicy = new RetryPolicy(5, BackoffPolicy.fixed(Duration.ofSeconds(2)));
		var definition = DurableProcessDefinition.of("submit-order", OrderSubmitted.class, BrokerPayload.class);

		var taskId = starter.start(new DurableProcessStartRequest<>(
				definition,
				new OrderSubmitted("order-1"),
				new BrokerPayload("payload-1"),
				new CorrelationId("corr-1"),
				retryPolicy));

		assertEquals(taskId, repository.savedTasks.getFirst().taskId());
		assertEquals(DurableProcessTaskStatus.PENDING, repository.savedTasks.getFirst().status());
		assertEquals(retryPolicy, repository.savedTasks.getFirst().retryPolicy());
		assertEquals("corr-1", repository.savedTasks.getFirst().correlationId().value());
		assertTrue(repository.savedTasks.getFirst().nextAttemptAt().isEmpty());
	}

	private record OrderSubmitted(String orderId) implements DurableProcessTrigger
	{
	}

	private record BrokerPayload(String value) implements DurableProcessPayload
	{
	}

	private static final class RecordingRepository implements DurableProcessTaskRepository
	{
		private final List<de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask>
				savedTasks =
				new ArrayList<>();

		@Override
		public de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask save(
				final de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask task)
		{
			savedTasks.add(task);
			return task;
		}

		@Override
		public de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask update(
				final de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask task)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask> findById(
				final de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId taskId)
		{
			return Optional.empty();
		}

		@Override
		public Collection<de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask> findDueTasks(
				final Instant asOf,
				final int limit)
		{
			return List.of();
		}
	}
}