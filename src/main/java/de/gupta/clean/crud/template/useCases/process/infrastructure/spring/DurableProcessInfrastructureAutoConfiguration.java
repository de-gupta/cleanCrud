package de.gupta.clean.crud.template.useCases.process.infrastructure.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.process.application.dispatch.ApplicationActionDispatcher;
import de.gupta.clean.crud.template.useCases.process.application.execution.DefaultDurableProcessRunner;
import de.gupta.clean.crud.template.useCases.process.application.execution.DurableProcessExecutionNudge;
import de.gupta.clean.crud.template.useCases.process.application.execution.DurableProcessRunner;
import de.gupta.clean.crud.template.useCases.process.application.execution.ImmediateDurableProcessExecutionNudge;
import de.gupta.clean.crud.template.useCases.process.application.registration.*;
import de.gupta.clean.crud.template.useCases.process.infrastructure.persistence.JpaDurableProcessTaskStore;
import de.gupta.clean.crud.template.useCases.process.infrastructure.persistence.model.DurableProcessTaskPersistenceModel;
import de.gupta.clean.crud.template.useCases.process.port.persistence.DurableProcessTaskRepository;
import de.gupta.clean.crud.template.useCases.process.port.scheduling.DurableProcessTaskScheduler;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.util.Collection;

@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@EnableScheduling
@EntityScan(basePackageClasses = DurableProcessTaskPersistenceModel.class)
@EnableConfigurationProperties(DurableProcessInfrastructureProperties.class)
public class DurableProcessInfrastructureAutoConfiguration
{
	@Bean
	@ConditionalOnMissingBean
	Clock durableProcessClock()
	{
		return Clock.systemUTC();
	}

	@Bean
	@ConditionalOnMissingBean
	ApplicationActionDispatcher applicationActionDispatcher()
	{
		return _ ->
		{
		};
	}

	@Bean
	@ConditionalOnMissingBean
	DurableProcessDefinitionRegistry durableProcessDefinitionRegistry(
			final Collection<DurableRegisteredProcess<?, ?>> registeredProcesses)
	{
		return DefaultDurableProcessDefinitionRegistry.of(registeredProcesses);
	}

	@Configuration
	@ConditionalOnProperty(prefix = "clean-crud.process", name = "enabled", havingValue = "true",
			matchIfMissing = true)
	@ConditionalOnBean(EntityManagerFactory.class)
	static class EnabledDurableProcessInfrastructureConfiguration
	{
		@Bean
		@ConditionalOnMissingBean
		JpaDurableProcessTaskStore jpaDurableProcessTaskStore(
				final EntityManager entityManager,
				final ObjectMapper objectMapper)
		{
			return JpaDurableProcessTaskStore.with(entityManager, objectMapper);
		}

		@Bean
		@ConditionalOnMissingBean
		DurableProcessTaskRepository durableProcessTaskRepository(final JpaDurableProcessTaskStore taskStore)
		{
			return taskStore;
		}

		@Bean
		@ConditionalOnMissingBean
		DurableProcessTaskScheduler durableProcessTaskScheduler(final JpaDurableProcessTaskStore taskStore)
		{
			return taskStore;
		}

		@Bean
		@ConditionalOnMissingBean
		DurableProcessStarter durableProcessStarter(
				final DurableProcessTaskRepository taskRepository,
				final Clock durableProcessClock)
		{
			return DefaultDurableProcessStarter.with(taskRepository, durableProcessClock);
		}

		@Bean
		@ConditionalOnMissingBean
		DurableProcessRunner durableProcessRunner(
				final DurableProcessDefinitionRegistry definitionRegistry,
				final DurableProcessTaskRepository taskRepository,
				final DurableProcessTaskScheduler taskScheduler,
				final ApplicationActionDispatcher applicationActionDispatcher,
				final Clock durableProcessClock)
		{
			return DefaultDurableProcessRunner.with(
					definitionRegistry,
					taskRepository,
					taskScheduler,
					applicationActionDispatcher,
					durableProcessClock);
		}

		@Bean
		@ConditionalOnMissingBean
		DurableProcessExecutionNudge durableProcessExecutionNudge(
				final DurableProcessRunner durableProcessRunner,
				final Clock durableProcessClock)
		{
			return ImmediateDurableProcessExecutionNudge.with(durableProcessRunner, durableProcessClock);
		}

		@Bean
		@ConditionalOnProperty(prefix = "clean-crud.process", name = "polling-enabled", havingValue = "true",
				matchIfMissing = true)
		DurableProcessPollingScheduler durableProcessPollingScheduler(
				final DurableProcessRunner durableProcessRunner,
				final Clock durableProcessClock,
				final DurableProcessInfrastructureProperties properties)
		{
			return DurableProcessPollingScheduler.with(durableProcessRunner, durableProcessClock, properties);
		}
	}

	@Configuration
	@ConditionalOnProperty(prefix = "clean-crud.process", name = "enabled", havingValue = "false")
	static class DisabledDurableProcessInfrastructureConfiguration
	{
		@Bean
		@ConditionalOnMissingBean
		DurableProcessStarter durableProcessStarter()
		{
			return NoopDurableProcessStarter.create();
		}

		@Bean
		@ConditionalOnMissingBean
		DurableProcessRunner durableProcessRunner()
		{
			return NoopDurableProcessRunner.create();
		}

		@Bean
		@ConditionalOnMissingBean
		DurableProcessExecutionNudge durableProcessExecutionNudge()
		{
			return DurableProcessExecutionNudge.noop();
		}
	}
}