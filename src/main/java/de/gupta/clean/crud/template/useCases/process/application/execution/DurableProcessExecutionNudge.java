package de.gupta.clean.crud.template.useCases.process.application.execution;

import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;

import java.util.Collection;

@FunctionalInterface
public interface DurableProcessExecutionNudge
{
	static DurableProcessExecutionNudge noop()
	{
		return _ ->
		{
		};
	}

	void afterCommit(Collection<DurableProcessTaskId> taskIds);
}