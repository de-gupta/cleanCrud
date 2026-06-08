package de.gupta.clean.crud.template.useCases.process.infrastructure.spring;

import de.gupta.clean.crud.template.useCases.process.application.execution.DurableProcessRunner;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

final class NoopDurableProcessRunner implements DurableProcessRunner
{
	static NoopDurableProcessRunner create()
	{
		return new NoopDurableProcessRunner();
	}

	@Override
	public Collection<DurableProcessTask> runDueProcesses(final Instant asOf, final int limit)
	{
		return List.of();
	}

	@Override
	public Optional<DurableProcessTask> runTask(final DurableProcessTaskId taskId, final Instant asOf)
	{
		return Optional.empty();
	}

	private NoopDurableProcessRunner()
	{
	}
}
