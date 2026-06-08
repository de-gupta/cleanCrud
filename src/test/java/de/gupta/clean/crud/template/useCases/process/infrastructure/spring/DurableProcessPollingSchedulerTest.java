package de.gupta.clean.crud.template.useCases.process.infrastructure.spring;

import de.gupta.clean.crud.template.useCases.process.application.dispatch.ApplicationActionDispatcher;
import de.gupta.clean.crud.template.useCases.process.application.execution.DefaultDurableProcessRunner;
import de.gupta.clean.crud.template.useCases.process.application.execution.DurableProcessRunner;
import de.gupta.clean.crud.template.useCases.process.application.registration.DefaultDurableProcessDefinitionRegistry;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableRegisteredProcess;
import de.gupta.clean.crud.template.useCases.process.domain.action.ApplicationCommand;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessDefinition;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessTrigger;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.CorrelationId;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;
import de.gupta.clean.crud.template.useCases.process.domain.model.outcome.DurableProcessOutcome;
import de.gupta.clean.crud.template.useCases.process.domain.model.outcome.FailureClassification;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.BackoffPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.RetryPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTaskStatus;
import de.gupta.clean.crud.template.useCases.process.port.persistence.DurableProcessTaskRepository;
import de.gupta.clean.crud.template.useCases.process.port.scheduling.DurableProcessTaskScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class DurableProcessPollingSchedulerTest
{
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withBean(Clock.class, () -> Clock.fixed(Instant.parse("2026-06-08T10:00:00Z"), ZoneOffset.UTC))
			.withBean(DurableProcessRunner.class, RecordingRunner::new)
			.withUserConfiguration(TestConfiguration.class);

	@Test
	void pollingSchedulerInvokesRunnerWithConfiguredBatchSize()
	{
		contextRunner
				.withPropertyValues("clean-crud.process.batch-size=37")
				.run(context ->
				{
					var scheduler = DurableProcessPollingScheduler.with(
							context.getBean(DurableProcessRunner.class),
							context.getBean(Clock.class),
							context.getBean(DurableProcessInfrastructureProperties.class));
					var runner = context.getBean(RecordingRunner.class);

					scheduler.pollDueProcesses();

					assertThat(runner.invocations).containsExactly("2026-06-08T10:00:00Z#37");
				});
	}

	@Test
	void autoConfigurationDoesNotCreatePollingSchedulerWhenPollingIsDisabled()
	{
		contextRunner
				.withPropertyValues(
						"clean-crud.process.enabled=false",
						"clean-crud.process.polling-enabled=false")
				.withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
						DurableProcessInfrastructureAutoConfiguration.class))
				.run(context -> assertThat(context).doesNotHaveBean(DurableProcessPollingScheduler.class));
	}

	@Test
	void pollingSchedulerPicksUpRetryableTasksOnALaterPoll()
	{
		var clock = new MutableClock(Instant.parse("2026-06-08T10:00:00Z"));
		var dispatcher = new RecordingDispatcher();
		var taskStore = new InMemoryTaskStore();
		var runner = DefaultDurableProcessRunner.with(
				DefaultDurableProcessDefinitionRegistry.of(List.of(new DurableRegisteredProcess<>(
						DurableProcessDefinition.of("retry-once", PrintRequested.class, PrintPayload.class),
						(payload, context) ->
						{
							if (!context.isRetryAttempt())
							{
								return DurableProcessOutcome.retryAt(
										context.startedAt().plusMillis(200),
										FailureClassification.TRANSIENT_TECHNICAL_FAILURE,
										"temporary printer outage");
							}
							return DurableProcessOutcome.succeeded(
									List.of(new PrintedCommand(payload.value())),
									"PRINTED");
						}))),
				taskStore,
				taskStore,
				dispatcher,
				clock);
		var scheduler = DurableProcessPollingScheduler.with(
				runner,
				clock,
				new DurableProcessInfrastructureProperties());
		var originalTask = taskStore.save(new DurableProcessTask(
				new DurableProcessTaskId("task-1"),
				"retry-once",
				new CorrelationId("corr-task-1"),
				new PrintPayload("payload-task-1"),
				new RetryPolicy(3, BackoffPolicy.fixed(Duration.ofMillis(200))),
				DurableProcessTaskStatus.PENDING,
				0,
				Optional.empty(),
				clock.instant(),
				clock.instant(),
				Optional.empty(),
				Optional.empty()));

		scheduler.pollDueProcesses();
		var waitingRetryTask = taskStore.findById(originalTask.taskId()).orElseThrow();

		assertThat(waitingRetryTask.status()).isEqualTo(DurableProcessTaskStatus.WAITING_RETRY);
		assertThat(waitingRetryTask.attemptCount()).isEqualTo(1);
		assertThat(dispatcher.actions).isEmpty();

		clock.advanceBy(Duration.ofMillis(250));
		scheduler.pollDueProcesses();
		var succeededTask = taskStore.findById(originalTask.taskId()).orElseThrow();

		assertThat(succeededTask.status()).isEqualTo(DurableProcessTaskStatus.SUCCEEDED);
		assertThat(succeededTask.attemptCount()).isEqualTo(2);
		assertThat(dispatcher.actions).containsExactly(new PrintedCommand("payload-task-1"));
	}

	@org.springframework.boot.context.properties.EnableConfigurationProperties(DurableProcessInfrastructureProperties.class)
	static class TestConfiguration
	{
	}

	static final class RecordingRunner implements DurableProcessRunner
	{
		private final List<String> invocations = new java.util.ArrayList<>();

		@Override
		public java.util.Collection<de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask> runDueProcesses(
				final Instant asOf,
				final int limit)
		{
			invocations.add(asOf + "#" + limit);
			return List.of();
		}

		@Override
		public java.util.Optional<de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask> runTask(
				final de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId taskId,
				final Instant asOf)
		{
			return java.util.Optional.empty();
		}
	}

	private record PrintRequested(String taskId) implements DurableProcessTrigger
	{
	}

	private record PrintPayload(String value) implements DurableProcessPayload
	{
	}

	private record PrintedCommand(String value) implements ApplicationCommand
	{
	}

	private static final class RecordingDispatcher implements ApplicationActionDispatcher
	{
		private final List<de.gupta.clean.crud.template.useCases.process.domain.action.ApplicationAction> actions =
				new java.util.ArrayList<>();

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

	private static final class MutableClock extends Clock
	{
		private Instant currentInstant;

		@Override
		public ZoneOffset getZone()
		{
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(final java.time.ZoneId zone)
		{
			throw new UnsupportedOperationException("Zone changes are not supported in this test clock");
		}

		@Override
		public Instant instant()
		{
			return currentInstant;
		}

		private void advanceBy(final Duration duration)
		{
			currentInstant = currentInstant.plus(duration);
		}

		private MutableClock(final Instant currentInstant)
		{
			this.currentInstant = currentInstant;
		}
	}
}