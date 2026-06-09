package de.gupta.clean.crud.template.useCases.process.port.scheduling;

import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask;

import java.time.Instant;
import java.util.Collection;

public interface DurableProcessTaskScheduler
{
	Collection<DurableProcessTask> findDueTasks(Instant asOf, int limit);

	DurableProcessTask scheduleRetry(DurableProcessTask task, Instant nextAttemptAt);
}
