package de.gupta.clean.crud.template.useCases.process.infrastructure.spring;

import de.gupta.clean.crud.template.useCases.process.application.execution.DurableProcessRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.util.Objects;

public final class DurableProcessPollingScheduler
{
	private static final Logger log = LoggerFactory.getLogger(DurableProcessPollingScheduler.class);

	private final DurableProcessRunner durableProcessRunner;
	private final Clock clock;
	private final DurableProcessInfrastructureProperties properties;

	public static DurableProcessPollingScheduler with(
			final DurableProcessRunner durableProcessRunner,
			final Clock clock,
			final DurableProcessInfrastructureProperties properties)
	{
		return new DurableProcessPollingScheduler(durableProcessRunner, clock, properties);
	}

	@Scheduled(fixedDelayString = "${clean-crud.process.poll-interval:PT1M}")
	public void pollDueProcesses()
	{
		try
		{
			durableProcessRunner.runDueProcesses(clock.instant(), properties.batchSize());
		}
		catch (RuntimeException e)
		{
			log.warn("Durable process polling failed", e);
		}
	}

	private DurableProcessPollingScheduler(
			final DurableProcessRunner durableProcessRunner,
			final Clock clock,
			final DurableProcessInfrastructureProperties properties)
	{
		this.durableProcessRunner = Objects.requireNonNull(durableProcessRunner, "durableProcessRunner");
		this.clock = Objects.requireNonNull(clock, "clock");
		this.properties = Objects.requireNonNull(properties, "properties");
	}
}