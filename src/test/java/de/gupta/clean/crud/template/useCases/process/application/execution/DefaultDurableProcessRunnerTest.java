package de.gupta.clean.crud.template.useCases.process.application.execution;

import de.gupta.clean.crud.template.useCases.process.application.dispatch.ApplicationActionDispatcher;
import de.gupta.clean.crud.template.useCases.process.application.registration.DefaultDurableProcessDefinitionRegistry;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableRegisteredProcess;
import de.gupta.clean.crud.template.useCases.process.domain.action.ApplicationCommand;
import de.gupta.clean.crud.template.useCases.process.domain.action.ApplicationEvent;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessDefinition;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessTrigger;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.CorrelationId;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;
import de.gupta.clean.crud.template.useCases.process.domain.model.outcome.DurableProcessOutcome;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.BackoffPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.RetryPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTaskStatus;
import de.gupta.clean.crud.template.useCases.process.port.persistence.DurableProcessTaskRepository;
import de.gupta.clean.crud.template.useCases.process.port.scheduling.DurableProcessTaskScheduler;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultDurableProcessRunnerTest
{
	@Test
	void runnerMarksSuccessfulTasksAndDispatchesApplicationActions()
	{
		var repository = new InMemoryTaskStore();
		var dispatcher = new RecordingDispatcher();
		var definition = DurableProcessDefinition.of("submit-order", OrderSubmitted.class, BrokerPayload.class);
		var registry = DefaultDurableProcessDefinitionRegistry.of(List.of(new DurableRegisteredProcess<>(
				definition,
				(payload, _) -> DurableProcessOutcome.succeeded(List.of(
								new SubmittedCommand(payload.value()),
								new AcknowledgedEvent(payload.value())),
						"ACK"))));
		var runner = DefaultDurableProcessRunner.with(
				registry,
				repository,
				repository,
				dispatcher,
				Clock.fixed(Instant.parse("2026-06-08T10:05:00Z"), ZoneOffset.UTC));
		var task = repository.save(pendingTask("task-1", "submit-order", 0, Optional.empty()));

		var completed = runner.runTask(task.taskId(), Instant.parse("2026-06-08T10:00:00Z")).orElseThrow();

		assertEquals(DurableProcessTaskStatus.SUCCEEDED, completed.status());
		assertEquals(1, completed.attemptCount());
		assertEquals("ACK", completed.lastOutcomeCode().orElseThrow());
		assertEquals(2, dispatcher.actions.size());
	}

	@Test
	void runnerSchedulesRetryForRetryOutcomeWhenPolicyAllowsIt()
	{
		var repository = new InMemoryTaskStore();
		var dispatcher = new RecordingDispatcher();
		var definition = DurableProcessDefinition.of("submit-order", OrderSubmitted.class, BrokerPayload.class);
		var registry = DefaultDurableProcessDefinitionRegistry.of(List.of(new DurableRegisteredProcess<>(
				definition,
				(_, _) -> DurableProcessOutcome.retryAt(
						Instant.parse("2026-06-08T10:10:00Z"),
						de.gupta.clean.crud.template.useCases.process.domain.model.outcome.FailureClassification.TRANSIENT_TECHNICAL_FAILURE,
						"temporary broker outage"))));
		var runner = DefaultDurableProcessRunner.with(
				registry,
				repository,
				repository,
				dispatcher,
				Clock.fixed(Instant.parse("2026-06-08T10:05:00Z"), ZoneOffset.UTC));
		var task = repository.save(pendingTask("task-2", "submit-order", 0, Optional.empty()));

		var completed = runner.runTask(task.taskId(), Instant.parse("2026-06-08T10:00:00Z")).orElseThrow();

		assertEquals(DurableProcessTaskStatus.WAITING_RETRY, completed.status());
		assertEquals(1, completed.attemptCount());
		assertEquals(Instant.parse("2026-06-08T10:10:00Z"), completed.nextAttemptAt().orElseThrow());
		assertTrue(dispatcher.actions.isEmpty());
	}

	@Test
	void runnerFailsTaskWhenRetriesAreExhausted()
	{
		var repository = new InMemoryTaskStore();
		var dispatcher = new RecordingDispatcher();
		var definition = DurableProcessDefinition.of("submit-order", OrderSubmitted.class, BrokerPayload.class);
		var registry = DefaultDurableProcessDefinitionRegistry.of(List.of(new DurableRegisteredProcess<>(
				definition,
				(_, _) -> DurableProcessOutcome.retryAt(
						Instant.parse("2026-06-08T10:10:00Z"),
						de.gupta.clean.crud.template.useCases.process.domain.model.outcome.FailureClassification.TRANSIENT_TECHNICAL_FAILURE,
						"temporary broker outage"))));
		var runner = DefaultDurableProcessRunner.with(
				registry,
				repository,
				repository,
				dispatcher,
				Clock.fixed(Instant.parse("2026-06-08T10:05:00Z"), ZoneOffset.UTC));
		var task = repository.save(pendingTask("task-3", "submit-order", 1, Optional.empty(), 1));

		var completed = runner.runTask(task.taskId(), Instant.parse("2026-06-08T10:00:00Z")).orElseThrow();

		assertEquals(DurableProcessTaskStatus.FAILED, completed.status());
		assertEquals(2, completed.attemptCount());
	}

	@Test
	void runnerMarksTaskFailedWhenNoProcessIsRegistered()
	{
		var repository = new InMemoryTaskStore();
		var dispatcher = new RecordingDispatcher();
		var runner = DefaultDurableProcessRunner.with(
				DefaultDurableProcessDefinitionRegistry.of(List.of()),
				repository,
				repository,
				dispatcher,
				Clock.fixed(Instant.parse("2026-06-08T10:05:00Z"), ZoneOffset.UTC));
		var task = repository.save(pendingTask("task-4", "submit-order", 0, Optional.empty()));

		var completed = runner.runTask(task.taskId(), Instant.parse("2026-06-08T10:00:00Z")).orElseThrow();

		assertEquals(DurableProcessTaskStatus.FAILED, completed.status());
		assertEquals("PROCESS_NOT_REGISTERED", completed.lastOutcomeCode().orElseThrow());
	}

	private DurableProcessTask pendingTask(
			final String id,
			final String processType,
			final int attemptCount,
			final Optional<Instant> nextAttemptAt)
	{
		return pendingTask(id, processType, attemptCount, nextAttemptAt, 2);
	}

	private DurableProcessTask pendingTask(
			final String id,
			final String processType,
			final int attemptCount,
			final Optional<Instant> nextAttemptAt,
			final int maxAttempts)
	{
		return new DurableProcessTask(
				new DurableProcessTaskId(id),
				processType,
				new CorrelationId("corr-" + id),
				new BrokerPayload("payload-" + id),
				new RetryPolicy(maxAttempts, BackoffPolicy.fixed(Duration.ofSeconds(5))),
				nextAttemptAt.isPresent() ? DurableProcessTaskStatus.WAITING_RETRY : DurableProcessTaskStatus.PENDING,
				attemptCount,
				nextAttemptAt,
				Instant.parse("2026-06-08T10:00:00Z"),
				Instant.parse("2026-06-08T10:00:00Z"),
				Optional.empty(),
				Optional.empty());
	}

	private record OrderSubmitted(String orderId) implements DurableProcessTrigger
	{
	}

	private record BrokerPayload(String value) implements DurableProcessPayload
	{
	}

	private record SubmittedCommand(String value) implements ApplicationCommand
	{
	}

	private record AcknowledgedEvent(String value) implements ApplicationEvent
	{
	}

	private static final class RecordingDispatcher implements ApplicationActionDispatcher
	{
		private final List<de.gupta.clean.crud.template.useCases.process.domain.action.ApplicationAction> actions =
				new ArrayList<>();

		@Override
		public void dispatch(
				final Collection<? extends de.gupta.clean.crud.template.useCases.process.domain.action.ApplicationAction> applicationActions)
		{
			actions.addAll(applicationActions);
		}
	}

	private static final class InMemoryTaskStore implements DurableProcessTaskRepository, DurableProcessTaskScheduler
	{
		private final Map<DurableProcessTaskId, DurableProcessTask> tasks = new LinkedHashMap<>();

		@Override
		public DurableProcessTask save(final DurableProcessTask task)
		{
			tasks.put(task.taskId(), task);
			return task;
		}

		@Override
		public DurableProcessTask update(final DurableProcessTask task)
		{
			tasks.put(task.taskId(), task);
			return task;
		}

		@Override
		public Optional<DurableProcessTask> findById(final DurableProcessTaskId taskId)
		{
			return Optional.ofNullable(tasks.get(taskId));
		}

		@Override
		public Collection<DurableProcessTask> findDueTasks(final Instant asOf, final int limit)
		{
			return tasks.values().stream()
			            .filter(task -> task.isDueAt(asOf))
			            .limit(limit)
			            .toList();
		}

		@Override
		public DurableProcessTask scheduleRetry(final DurableProcessTask task, final Instant nextAttemptAt)
		{
			tasks.put(task.taskId(), task);
			return task;
		}
	}
}