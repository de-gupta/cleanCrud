package de.gupta.clean.crud.template.useCases.process.domain.model.task;

public enum DurableProcessTaskStatus
{
	PENDING,
	RUNNING,
	WAITING_RETRY,
	SUCCEEDED,
	REJECTED,
	FAILED,
	CANCELLED;

	public boolean isTerminal()
	{
		return switch (this)
		{
			case SUCCEEDED, REJECTED, FAILED, CANCELLED -> true;
			case PENDING, RUNNING, WAITING_RETRY -> false;
		};
	}

	public boolean isReadyForExecution()
	{
		return switch (this)
		{
			case PENDING, WAITING_RETRY -> true;
			case RUNNING, SUCCEEDED, REJECTED, FAILED, CANCELLED -> false;
		};
	}
}
