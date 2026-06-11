package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.infrastructure.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.operation.creation.application.service.QuarantinableCreationService;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.DefaultCreationQuarantineService;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.recording.CreationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.quarantine.api.application.QuarantineApplicationController;
import de.gupta.clean.crud.template.useCases.operation.quarantine.api.application.QuarantineApplicationControllers;
import de.gupta.clean.crud.template.useCases.operation.quarantine.api.web.DefaultSpringRestCreationQuarantineController;
import de.gupta.clean.crud.template.useCases.operation.quarantine.api.web.QuarantineWebMapper;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.QuarantineReplayCodec;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.QuarantineReplayGateway;
import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.QuarantineService;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.CreationReplayInputs;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.policy.QuarantineAccessPolicy;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.port.QuarantineRepository;
import de.gupta.clean.crud.template.useCases.operation.quarantine.infrastructure.persistence.JacksonQuarantineReplayCodec;
import de.gupta.clean.crud.template.useCases.operation.quarantine.infrastructure.persistence.JpaQuarantineStore;
import de.gupta.clean.crud.template.useCases.operation.quarantine.infrastructure.persistence.model.CreationQuarantineEntity;
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
@EntityScan(basePackageClasses = CreationQuarantineEntity.class)
@EnableConfigurationProperties(CreationQuarantineInfrastructureProperties.class)
public class CreationQuarantineInfrastructureAutoConfiguration
{
	@Bean
	@ConditionalOnMissingBean
	QuarantineAccessPolicy<CreationReplayInputs> creationQuarantineAccessPolicy()
	{
		return QuarantineAccessPolicy.allowing();
	}

	@Configuration
	@ConditionalOnProperty(
			prefix = "clean-crud.creation.quarantine",
			name = "enabled",
			havingValue = "true",
			matchIfMissing = true)
	@ConditionalOnBean(EntityManagerFactory.class)
	static class EnabledCreationQuarantineInfrastructureConfiguration
	{
		@Bean
		@ConditionalOnMissingBean
		QuarantineRepository<CreationReplayInputs> creationQuarantineRepository(
				final EntityManager entityManager,
				final ObjectMapper objectMapper)
		{
			return JpaQuarantineStore.with(entityManager, objectMapper,
					CreationQuarantineEntity.class, CreationReplayInputs.class);
		}

		@Bean
		@ConditionalOnMissingBean
		QuarantineReplayCodec creationQuarantineReplayCodec(final ObjectMapper objectMapper)
		{
			return JacksonQuarantineReplayCodec.with(objectMapper);
		}

		@Bean
		@ConditionalOnMissingBean
		@SuppressWarnings("unchecked")
		DefaultCreationQuarantineService creationQuarantineService(
				final QuarantineRepository<CreationReplayInputs> repository,
				final ListableBeanFactory beanFactory,
				final QuarantineReplayCodec replayCodec,
				final Clock durableProcessClock)
		{
			return DefaultCreationQuarantineService.with(repository,
					aggregateKey -> DefaultCreationQuarantineService.replayRegistry(
																			beanFactory.getBeansOfType(QuarantinableCreationService.class)
							                                                           .values()
							                                                           .stream()
							                                                           .map(service -> (QuarantineReplayGateway<ApplicationOperationPayload>) service)
							                                                           .toList())
					                                                .findGateway(aggregateKey),
					replayCodec, durableProcessClock);
		}

		@Bean
		@ConditionalOnMissingBean
		CreationQuarantineRecorder creationQuarantineRecorder(
				final ObjectProvider<DefaultCreationQuarantineService> creationQuarantineService)
		{
			return submission -> creationQuarantineService.getObject().record(submission);
		}

		@Bean
		@ConditionalOnMissingBean
		QuarantineApplicationController<CreationReplayInputs> creationQuarantineApplicationController(
				final QuarantineService<CreationReplayInputs> service,
				final QuarantineAccessPolicy<CreationReplayInputs> accessPolicy)
		{
			return QuarantineApplicationControllers.controller(service, accessPolicy);
		}

		@Bean
		@ConditionalOnMissingBean
		QuarantineWebMapper quarantineWebMapper()
		{
			return new QuarantineWebMapper();
		}

		@Bean
		@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
		@ConditionalOnProperty(prefix = "clean-crud.creation.quarantine", name = "api-enabled", havingValue = "true")
		@ConditionalOnMissingBean
		DefaultSpringRestCreationQuarantineController springRestCreationQuarantineController(
				final QuarantineApplicationController<CreationReplayInputs> applicationController,
				final QuarantineWebMapper webMapper)
		{
			return new DefaultSpringRestCreationQuarantineController(applicationController, webMapper);
		}
	}

	@Configuration
	@ConditionalOnProperty(prefix = "clean-crud.creation.quarantine", name = "enabled", havingValue = "false")
	static class DisabledCreationQuarantineInfrastructureConfiguration
	{
		@Bean
		@ConditionalOnMissingBean
		CreationQuarantineRecorder creationQuarantineRecorder()
		{
			return CreationQuarantineRecorder.noop();
		}
	}
}