package de.gupta.clean.crud.template.useCases.process.application.execution;

import de.gupta.clean.crud.template.useCases.process.domain.model.id.CorrelationId;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;

import java.time.Instant;
import java.util.Objects;

public record DurableProcessExecutionContext(
		DurableProcessTaskId taskId,
		String processType,
		CorrelationId correlationId,
		int attemptNumber,
		Instant startedAt)
{
	public DurableProcessExecutionContext
	{
		Objects.requireNonNull(taskId, "taskId");
		if (processType == null || processType.isBlank())
		{
			throw new IllegalArgumentException("processType");
		}
		Objects.requireNonNull(correlationId, "correlationId");
		if (attemptNumber < 1)
		{
			throw new IllegalArgumentException("attemptNumber");
		}
		Objects.requireNonNull(startedAt, "startedAt");
	}

	public boolean isRetryAttempt()
	{
		return attemptNumber > 1;
	}
}
