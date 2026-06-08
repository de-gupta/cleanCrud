package de.gupta.clean.crud.template.useCases.process.port.persistence;

import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface DurableProcessTaskRepository
{
	DurableProcessTask save(DurableProcessTask task);

	DurableProcessTask update(DurableProcessTask task);

	Optional<DurableProcessTask> findById(DurableProcessTaskId taskId);

	Collection<DurableProcessTask> findDueTasks(Instant asOf, int limit);
}
