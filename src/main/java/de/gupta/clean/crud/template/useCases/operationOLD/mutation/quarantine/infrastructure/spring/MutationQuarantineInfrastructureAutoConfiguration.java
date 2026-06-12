package de.gupta.clean.crud.template.useCases.operationOLD.mutation.quarantine.infrastructure.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.application.service.QuarantinableMutationService;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.quarantine.application.DefaultMutationQuarantineService;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.quarantine.application.recording.MutationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.api.application.QuarantineApplicationController;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.api.application.QuarantineApplicationControllers;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.api.web.DefaultSpringRestMutationQuarantineController;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.api.web.QuarantineWebMapper;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service.QuarantineReplayCodec;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service.QuarantineReplayGateway;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service.QuarantineService;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.MutationReplayData;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.MutationReplayInputs;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.policy.QuarantineAccessPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.port.QuarantineRepository;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.infrastructure.persistence.JacksonQuarantineReplayCodec;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.infrastructure.persistence.JpaQuarantineStore;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.infrastructure.persistence.model.MutationQuarantineEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
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

@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@EntityScan(basePackageClasses = MutationQuarantineEntity.class)
@EnableConfigurationProperties(MutationQuarantineInfrastructureProperties.class)
public class MutationQuarantineInfrastructureAutoConfiguration
{
	@Bean
	@ConditionalOnMissingBean
	QuarantineAccessPolicy<MutationReplayInputs> mutationQuarantineAccessPolicy()
	{
		return QuarantineAccessPolicy.allowing();
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
		QuarantineRepository<MutationReplayInputs> mutationQuarantineRepository(
				final EntityManager entityManager,
				final ObjectMapper objectMapper)
		{
			return JpaQuarantineStore.with(entityManager, objectMapper,
					MutationQuarantineEntity.class, MutationReplayInputs.class);
		}

		@Bean
		@ConditionalOnMissingBean
		QuarantineReplayCodec mutationQuarantineReplayCodec(final ObjectMapper objectMapper)
		{
			return JacksonQuarantineReplayCodec.with(objectMapper);
		}

		@Bean
		@ConditionalOnMissingBean
		@SuppressWarnings("unchecked")
		DefaultMutationQuarantineService mutationQuarantineService(
				final QuarantineRepository<MutationReplayInputs> repository,
				final ListableBeanFactory beanFactory,
				final QuarantineReplayCodec replayCodec,
				final Clock durableProcessClock)
		{
			return DefaultMutationQuarantineService.with(repository,
					aggregateKey -> DefaultMutationQuarantineService.replayRegistry(
																			beanFactory.getBeansOfType(QuarantinableMutationService.class)
							                                                           .values()
							                                                           .stream()
							                                                           .map(service -> (QuarantineReplayGateway<MutationReplayData>) service)
							                                                           .toList())
					                                                .findGateway(aggregateKey),
					replayCodec, durableProcessClock);
		}

		@Bean
		@ConditionalOnMissingBean
		MutationQuarantineRecorder mutationQuarantineRecorder(
				final ObjectProvider<DefaultMutationQuarantineService> mutationQuarantineService)
		{
			return submission -> mutationQuarantineService.getObject().record(submission);
		}

		@Bean
		@ConditionalOnMissingBean
		QuarantineApplicationController<MutationReplayInputs> mutationQuarantineApplicationController(
				final QuarantineService<MutationReplayInputs> service,
				final QuarantineAccessPolicy<MutationReplayInputs> accessPolicy)
		{
			return QuarantineApplicationControllers.controller(service, accessPolicy);
		}

		@Bean
		@ConditionalOnMissingBean
		QuarantineWebMapper mutationQuarantineWebMapper()
		{
			return new QuarantineWebMapper();
		}

		@Bean
		@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
		@ConditionalOnProperty(prefix = "clean-crud.mutation.quarantine", name = "api-enabled", havingValue = "true")
		@ConditionalOnMissingBean
		DefaultSpringRestMutationQuarantineController springRestMutationQuarantineController(
				final QuarantineApplicationController<MutationReplayInputs> applicationController,
				final QuarantineWebMapper webMapper)
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