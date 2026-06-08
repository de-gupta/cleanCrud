package de.gupta.clean.crud.template.useCases.process.application.execution;

import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface DurableProcessRunner
{
	Collection<DurableProcessTask> runDueProcesses(Instant asOf, int limit);

	Optional<DurableProcessTask> runTask(DurableProcessTaskId taskId, Instant asOf);
}
