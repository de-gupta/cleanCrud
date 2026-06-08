package de.gupta.clean.crud.template.useCases.process.infrastructure.spring;

import de.gupta.clean.crud.template.useCases.process.application.execution.DurableProcessRunner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DurableProcessPollingSchedulerTest
{
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withBean(Clock.class, () -> Clock.fixed(Instant.parse("2026-06-08T10:00:00Z"), ZoneOffset.UTC))
			.withBean(DurableProcessRunner.class, RecordingRunner::new)
			.withUserConfiguration(TestConfiguration.class);

	@Test
	void pollingSchedulerInvokesRunnerWithConfiguredBatchSize()
	{
		contextRunner
				.withPropertyValues("clean-crud.process.batch-size=37")
				.run(context ->
				{
					var scheduler = DurableProcessPollingScheduler.with(
							context.getBean(DurableProcessRunner.class),
							context.getBean(Clock.class),
							context.getBean(DurableProcessInfrastructureProperties.class));
					var runner = context.getBean(RecordingRunner.class);

					scheduler.pollDueProcesses();

					assertThat(runner.invocations).containsExactly("2026-06-08T10:00:00Z#37");
				});
	}

	@Test
	void autoConfigurationDoesNotCreatePollingSchedulerWhenPollingIsDisabled()
	{
		contextRunner
				.withPropertyValues(
						"clean-crud.process.enabled=false",
						"clean-crud.process.polling-enabled=false")
				.withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
						DurableProcessInfrastructureAutoConfiguration.class))
				.run(context -> assertThat(context).doesNotHaveBean(DurableProcessPollingScheduler.class));
	}

	@org.springframework.boot.context.properties.EnableConfigurationProperties(DurableProcessInfrastructureProperties.class)
	static class TestConfiguration
	{
	}

	static final class RecordingRunner implements DurableProcessRunner
	{
		private final List<String> invocations = new java.util.ArrayList<>();

		@Override
		public java.util.Collection<de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask> runDueProcesses(
				final Instant asOf,
				final int limit)
		{
			invocations.add(asOf + "#" + limit);
			return List.of();
		}

		@Override
		public java.util.Optional<de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask> runTask(
				final de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId taskId,
				final Instant asOf)
		{
			return java.util.Optional.empty();
		}
	}
}
