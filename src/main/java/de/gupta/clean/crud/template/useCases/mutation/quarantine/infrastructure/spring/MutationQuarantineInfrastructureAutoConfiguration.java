package de.gupta.clean.crud.template.useCases.mutation.quarantine.infrastructure.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.api.application.MutationQuarantineApplicationController;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.api.application.MutationQuarantineApplicationControllers;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.api.web.DefaultSpringRestMutationQuarantineController;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.api.web.MutationQuarantineWebMapper;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.application.*;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.application.recording.MutationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.policy.MutationQuarantineAccessPolicy;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.infrastructure.persistence.JpaMutationQuarantineStore;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.infrastructure.persistence.model.MutationQuarantinePersistenceModel;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.port.persistence.MutationQuarantineRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.Collection;

@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@EntityScan(basePackageClasses = MutationQuarantinePersistenceModel.class)
@EnableConfigurationProperties(MutationQuarantineInfrastructureProperties.class)
public class MutationQuarantineInfrastructureAutoConfiguration
{
	@Bean
	@ConditionalOnMissingBean
	MutationQuarantineReplayRegistry mutationQuarantineReplayRegistry(
			final Collection<MutationQuarantineReplayGateway> gateways)
	{
		return DefaultMutationQuarantineReplayRegistry.of(gateways);
	}

	@Bean
	@ConditionalOnMissingBean
	MutationQuarantineAccessPolicy mutationQuarantineAccessPolicy()
	{
		return MutationQuarantineAccessPolicy.allowing();
	}

	@Configuration
	@ConditionalOnProperty(
			prefix = "clean-crud.mutation.quarantine",
			name = "enabled",
			havingValue = "true",
			matchIfMissing = true)
	@ConditionalOnBean(EntityManagerFactory.class)
	static class EnabledMutationQuarantineInfrastructureConfiguration
	{
		@Bean
		@ConditionalOnMissingBean
		MutationQuarantineRepository mutationQuarantineRepository(
				final EntityManager entityManager,
				final ObjectMapper objectMapper)
		{
			return JpaMutationQuarantineStore.with(entityManager, objectMapper);
		}

		@Bean
		@ConditionalOnMissingBean
		MutationQuarantineService mutationQuarantineService(
				final MutationQuarantineRepository repository,
				final MutationQuarantineReplayRegistry replayRegistry,
				final ObjectMapper objectMapper,
				final Clock durableProcessClock)
		{
			return DefaultMutationQuarantineService.with(repository, replayRegistry, objectMapper, durableProcessClock);
		}

		@Bean
		@ConditionalOnMissingBean
		MutationQuarantineRecorder mutationQuarantineRecorder(final MutationQuarantineService mutationQuarantineService)
		{
			return mutationQuarantineService;
		}

		@Bean
		@ConditionalOnMissingBean
		MutationQuarantineApplicationController mutationQuarantineApplicationController(
				final MutationQuarantineService mutationQuarantineService,
				final MutationQuarantineAccessPolicy mutationQuarantineAccessPolicy)
		{
			return MutationQuarantineApplicationControllers.controller(
					mutationQuarantineService,
					mutationQuarantineAccessPolicy);
		}

		@Bean
		@ConditionalOnMissingBean
		MutationQuarantineWebMapper mutationQuarantineWebMapper()
		{
			return new MutationQuarantineWebMapper();
		}

		@Bean
		@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
		@ConditionalOnProperty(prefix = "clean-crud.mutation.quarantine", name = "api-enabled", havingValue = "true")
		@ConditionalOnMissingBean
		DefaultSpringRestMutationQuarantineController springRestMutationQuarantineController(
				final MutationQuarantineApplicationController applicationController,
				final MutationQuarantineWebMapper webMapper)
		{
			return new DefaultSpringRestMutationQuarantineController(applicationController, webMapper);
		}
	}

	@Configuration
	@ConditionalOnProperty(prefix = "clean-crud.mutation.quarantine", name = "enabled", havingValue = "false")
	static class DisabledMutationQuarantineInfrastructureConfiguration
	{
		@Bean
		@ConditionalOnMissingBean
		MutationQuarantineRecorder mutationQuarantineRecorder()
		{
			return MutationQuarantineRecorder.noop();
		}
	}
}