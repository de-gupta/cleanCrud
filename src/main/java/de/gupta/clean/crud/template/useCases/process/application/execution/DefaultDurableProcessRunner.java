package de.gupta.clean.crud.template.useCases.process.application.execution;

import de.gupta.clean.crud.template.useCases.process.application.dispatch.ApplicationActionDispatcher;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessDefinitionRegistry;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableRegisteredProcess;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.model.outcome.DurableProcessOutcome;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask;
import de.gupta.clean.crud.template.useCases.process.port.persistence.DurableProcessTaskRepository;
import de.gupta.clean.crud.template.useCases.process.port.scheduling.DurableProcessTaskScheduler;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public final class DefaultDurableProcessRunner implements DurableProcessRunner
{
	private final DurableProcessDefinitionRegistry definitionRegistry;
	private final DurableProcessTaskRepository taskRepository;
	private final DurableProcessTaskScheduler taskScheduler;
	private final ApplicationActionDispatcher actionDispatcher;
	private final Clock clock;

	public static DefaultDurableProcessRunner with(
			final DurableProcessDefinitionRegistry definitionRegistry,
			final DurableProcessTaskRepository taskRepository,
			final DurableProcessTaskScheduler taskScheduler,
			final ApplicationActionDispatcher actionDispatcher,
			final Clock clock)
	{
		return new DefaultDurableProcessRunner(definitionRegistry, taskRepository, taskScheduler, actionDispatcher,
				clock);
	}

	@Override
	public Collection<DurableProcessTask> runDueProcesses(final Instant asOf, final int limit)
	{
		Objects.requireNonNull(asOf, "asOf");
		if (limit < 1)
		{
			throw new IllegalArgumentException("limit");
		}
		return taskScheduler.findDueTasks(asOf, limit).stream()
		                    .map(task -> run(task, asOf))
		                    .toList();
	}

	@Override
	public Optional<DurableProcessTask> runTask(
			final de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId taskId,
			final Instant asOf)
	{
		Objects.requireNonNull(taskId, "taskId");
		Objects.requireNonNull(asOf, "asOf");
		return taskRepository.findById(taskId)
		                     .filter(task -> task.isDueAt(asOf))
		                     .map(task -> run(task, asOf));
	}

	private DurableProcessTask run(final DurableProcessTask task, final Instant asOf)
	{
		var startedTask = taskRepository.update(task.startAttempt(asOf));
		var completedAt = clock.instant();
		var registeredProcess = definitionRegistry.registeredProcess(startedTask.processType());
		if (registeredProcess.isEmpty())
		{
			return taskRepository.update(startedTask.markFailed(
					completedAt,
					Optional.of("No durable process registered for type " + startedTask.processType()),
					Optional.of("PROCESS_NOT_REGISTERED")));
		}
		try
		{
			var outcome = execute(startedTask, registeredProcess.orElseThrow(), asOf);
			var completedTask = persistOutcome(startedTask, outcome, completedAt);
			dispatch(outcome);
			return completedTask;
		}
		catch (RuntimeException e)
		{
			return taskRepository.update(startedTask.markFailed(
					completedAt,
					Optional.ofNullable(e.getMessage()).or(() -> Optional.of(e.getClass().getSimpleName())),
					Optional.of("PROCESS_EXECUTION_EXCEPTION")));
		}
	}

	private DurableProcessTask persistOutcome(
			final DurableProcessTask startedTask,
			final DurableProcessOutcome outcome,
			final Instant completedAt)
	{
		return switch (outcome)
		{
			case DurableProcessOutcome.Success success -> taskRepository.update(
					startedTask.markSucceeded(completedAt, success.outcomeCode()));
			case DurableProcessOutcome.Rejected rejected -> taskRepository.update(
					startedTask.markRejected(completedAt, rejected.failureSummary(), rejected.outcomeCode()));
			case DurableProcessOutcome.Failed failed -> taskRepository.update(
					startedTask.markFailed(completedAt, failed.failureSummary(), failed.outcomeCode()));
			case DurableProcessOutcome.Retry retry -> retryOutcome(startedTask, retry, completedAt);
		};
	}

	private DurableProcessTask retryOutcome(
			final DurableProcessTask startedTask,
			final DurableProcessOutcome.Retry retry,
			final Instant completedAt)
	{
		if (!startedTask.retryPolicy().allowsRetry(startedTask.attemptCount()))
		{
			return taskRepository.update(startedTask.markFailed(
					completedAt,
					retry.failureSummary(),
					retry.outcomeCode().or(() -> Optional.of("RETRIES_EXHAUSTED"))));
		}
		return taskScheduler.scheduleRetry(
				startedTask.scheduleRetry(completedAt, retry.nextAttemptAt(), retry.failureSummary(),
						retry.outcomeCode()),
				retry.nextAttemptAt());
	}

	private void dispatch(final DurableProcessOutcome outcome)
	{
		if (!outcome.applicationActions().isEmpty())
		{
			actionDispatcher.dispatch(outcome.applicationActions());
		}
	}

	@SuppressWarnings("unchecked")
	private DurableProcessOutcome execute(
			final DurableProcessTask startedTask,
			final DurableRegisteredProcess<?, ?> registeredProcess,
			final Instant startedAt)
	{
		var process = (DurableRegisteredProcess<?, DurableProcessPayload>) registeredProcess;
		var payload = process.definition().payloadType().cast(startedTask.payload());
		var context = new DurableProcessExecutionContext(
				startedTask.taskId(),
				startedTask.processType(),
				startedTask.correlationId(),
				startedTask.attemptCount(),
				startedAt);
		return process.executor().execute(payload, context);
	}

	private DefaultDurableProcessRunner(
			final DurableProcessDefinitionRegistry definitionRegistry,
			final DurableProcessTaskRepository taskRepository,
			final DurableProcessTaskScheduler taskScheduler,
			final ApplicationActionDispatcher actionDispatcher,
			final Clock clock)
	{
		this.definitionRegistry = Objects.requireNonNull(definitionRegistry, "definitionRegistry");
		this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository");
		this.taskScheduler = Objects.requireNonNull(taskScheduler, "taskScheduler");
		this.actionDispatcher = Objects.requireNonNull(actionDispatcher, "actionDispatcher");
		this.clock = Objects.requireNonNull(clock, "clock");
	}
}