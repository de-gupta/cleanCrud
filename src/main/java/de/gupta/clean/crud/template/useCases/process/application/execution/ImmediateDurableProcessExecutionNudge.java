package de.gupta.clean.crud.template.useCases.process.application.execution;

import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Collection;
import java.util.Objects;

public final class ImmediateDurableProcessExecutionNudge implements DurableProcessExecutionNudge
{
	private static final Logger log = LoggerFactory.getLogger(ImmediateDurableProcessExecutionNudge.class);

	private final DurableProcessRunner durableProcessRunner;
	private final Clock clock;

	public static ImmediateDurableProcessExecutionNudge with(
			final DurableProcessRunner durableProcessRunner,
			final Clock clock)
	{
		return new ImmediateDurableProcessExecutionNudge(durableProcessRunner, clock);
	}

	@Override
	public void afterCommit(final Collection<DurableProcessTaskId> taskIds)
	{
		Objects.requireNonNull(taskIds, "taskIds");
		var asOf = clock.instant();
		for (var taskId : taskIds)
		{
			try
			{
				durableProcessRunner.runTask(taskId, asOf);
			}
			catch (RuntimeException e)
			{
				log.warn("Immediate durable process execution failed for {}", taskId, e);
			}
		}
	}

	private ImmediateDurableProcessExecutionNudge(
			final DurableProcessRunner durableProcessRunner,
			final Clock clock)
	{
		this.durableProcessRunner = Objects.requireNonNull(durableProcessRunner, "durableProcessRunner");
		this.clock = Objects.requireNonNull(clock, "clock");
	}
}
