package de.gupta.clean.crud.template.useCases.process.application.registration;

import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTaskStatus;
import de.gupta.clean.crud.template.useCases.process.port.persistence.DurableProcessTaskRepository;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

public final class DefaultDurableProcessStarter implements DurableProcessStarter
{
	private final DurableProcessTaskRepository taskRepository;
	private final Clock clock;

	public static DefaultDurableProcessStarter with(
			final DurableProcessTaskRepository taskRepository,
			final Clock clock)
	{
		return new DefaultDurableProcessStarter(taskRepository, clock);
	}

	@Override
	public DurableProcessTaskId start(final DurableProcessStartRequest<?, ?> startRequest)
	{
		Objects.requireNonNull(startRequest, "startRequest");
		var now = clock.instant();
		var task = new DurableProcessTask(
				DurableProcessTaskId.random(),
				startRequest.definition().processType(),
				startRequest.correlationId(),
				startRequest.payload(),
				startRequest.retryPolicy(),
				DurableProcessTaskStatus.PENDING,
				0,
				Optional.empty(),
				now,
				now,
				Optional.empty(),
				Optional.empty());
		return taskRepository.save(task).taskId();
	}

	private DefaultDurableProcessStarter(
			final DurableProcessTaskRepository taskRepository,
			final Clock clock)
	{
		this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository");
		this.clock = Objects.requireNonNull(clock, "clock");
	}
}
