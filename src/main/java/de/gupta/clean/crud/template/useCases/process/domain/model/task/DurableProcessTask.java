package de.gupta.clean.crud.template.useCases.process.domain.model.task;

import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.CorrelationId;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.RetryPolicy;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record DurableProcessTask(
		DurableProcessTaskId taskId,
		String processType,
		CorrelationId correlationId,
		DurableProcessPayload payload,
		RetryPolicy retryPolicy,
		DurableProcessTaskStatus status,
		int attemptCount,
		Optional<Instant> nextAttemptAt,
		Instant createdAt,
		Instant updatedAt,
		Optional<String> lastFailureSummary,
		Optional<String> lastOutcomeCode)
{
	public DurableProcessTask
	{
		Objects.requireNonNull(taskId, "taskId");
		if (processType == null || processType.isBlank())
		{
			throw new IllegalArgumentException("processType");
		}
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(payload, "payload");
		Objects.requireNonNull(retryPolicy, "retryPolicy");
		Objects.requireNonNull(status, "status");
		if (attemptCount < 0)
		{
			throw new IllegalArgumentException("attemptCount");
		}
		nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt");
		Objects.requireNonNull(createdAt, "createdAt");
		Objects.requireNonNull(updatedAt, "updatedAt");
		lastFailureSummary = Objects.requireNonNull(lastFailureSummary, "lastFailureSummary");
		lastOutcomeCode = Objects.requireNonNull(lastOutcomeCode, "lastOutcomeCode");
	}

	public boolean isTerminal()
	{
		return status.isTerminal();
	}

	public boolean isDueAt(final Instant instant)
	{
		Objects.requireNonNull(instant, "instant");
		return status.isReadyForExecution() && nextAttemptAt.map(scheduled -> !scheduled.isAfter(instant)).orElse(true);
	}

	public DurableProcessTask startAttempt(final Instant startedAt)
	{
		Objects.requireNonNull(startedAt, "startedAt");
		if (!status.isReadyForExecution())
		{
			throw new IllegalStateException("Task is not ready for execution");
		}
		return new DurableProcessTask(
				taskId,
				processType,
				correlationId,
				payload,
				retryPolicy,
				DurableProcessTaskStatus.RUNNING,
				attemptCount + 1,
				Optional.empty(),
				createdAt,
				startedAt,
				Optional.empty(),
				Optional.empty());
	}

	public DurableProcessTask markSucceeded(final Instant completedAt, final Optional<String> outcomeCode)
	{
		Objects.requireNonNull(completedAt, "completedAt");
		Objects.requireNonNull(outcomeCode, "outcomeCode");
		return new DurableProcessTask(
				taskId,
				processType,
				correlationId,
				payload,
				retryPolicy,
				DurableProcessTaskStatus.SUCCEEDED,
				attemptCount,
				Optional.empty(),
				createdAt,
				completedAt,
				Optional.empty(),
				outcomeCode);
	}

	public DurableProcessTask scheduleRetry(
			final Instant completedAt,
			final Instant scheduledAt,
			final Optional<String> failureSummary,
			final Optional<String> outcomeCode)
	{
		Objects.requireNonNull(completedAt, "completedAt");
		Objects.requireNonNull(scheduledAt, "scheduledAt");
		Objects.requireNonNull(failureSummary, "failureSummary");
		Objects.requireNonNull(outcomeCode, "outcomeCode");
		return new DurableProcessTask(
				taskId,
				processType,
				correlationId,
				payload,
				retryPolicy,
				DurableProcessTaskStatus.WAITING_RETRY,
				attemptCount,
				Optional.of(scheduledAt),
				createdAt,
				completedAt,
				failureSummary,
				outcomeCode);
	}

	public DurableProcessTask markRejected(
			final Instant completedAt,
			final Optional<String> failureSummary,
			final Optional<String> outcomeCode)
	{
		Objects.requireNonNull(completedAt, "completedAt");
		Objects.requireNonNull(failureSummary, "failureSummary");
		Objects.requireNonNull(outcomeCode, "outcomeCode");
		return new DurableProcessTask(
				taskId,
				processType,
				correlationId,
				payload,
				retryPolicy,
				DurableProcessTaskStatus.REJECTED,
				attemptCount,
				Optional.empty(),
				createdAt,
				completedAt,
				failureSummary,
				outcomeCode);
	}

	public DurableProcessTask markFailed(
			final Instant completedAt,
			final Optional<String> failureSummary,
			final Optional<String> outcomeCode)
	{
		Objects.requireNonNull(completedAt, "completedAt");
		Objects.requireNonNull(failureSummary, "failureSummary");
		Objects.requireNonNull(outcomeCode, "outcomeCode");
		return new DurableProcessTask(
				taskId,
				processType,
				correlationId,
				payload,
				retryPolicy,
				DurableProcessTaskStatus.FAILED,
				attemptCount,
				Optional.empty(),
				createdAt,
				completedAt,
				failureSummary,
				outcomeCode);
	}
}
