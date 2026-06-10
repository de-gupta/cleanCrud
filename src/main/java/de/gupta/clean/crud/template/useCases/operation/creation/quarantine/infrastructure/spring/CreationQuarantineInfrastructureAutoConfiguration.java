package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.infrastructure.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.operation.creation.application.service.CreationService;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.application.CreationQuarantineApplicationController;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.application.CreationQuarantineApplicationControllers;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.web.CreationQuarantineWebMapper;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.web.DefaultSpringRestCreationQuarantineController;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.CreationQuarantinePayloadCodec;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.CreationQuarantineReplayGateway;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.CreationQuarantineReplayRegistry;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.CreationQuarantineService;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.DefaultCreationQuarantineService;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.recording.CreationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.policy.CreationQuarantineAccessPolicy;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.infrastructure.persistence.JacksonCreationQuarantinePayloadCodec;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.infrastructure.persistence.JpaCreationQuarantineStore;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.infrastructure.persistence.model.CreationQuarantinePersistenceModel;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.port.persistence.CreationQuarantineRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.ListableBeanFactory;
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
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@EntityScan(basePackageClasses = CreationQuarantinePersistenceModel.class)
@EnableConfigurationProperties(CreationQuarantineInfrastructureProperties.class)
public class CreationQuarantineInfrastructureAutoConfiguration
{
	@Bean
	@ConditionalOnMissingBean
	CreationQuarantineReplayRegistry creationQuarantineReplayRegistry(
			final ListableBeanFactory beanFactory)
	{
		return aggregateType -> java.util.Optional.ofNullable(discoverReplayGateways(beanFactory).get(aggregateType));
	}

	@Bean
	@ConditionalOnMissingBean
	CreationQuarantineAccessPolicy creationQuarantineAccessPolicy()
	{
		return CreationQuarantineAccessPolicy.allowing();
	}

	private static Map<String, CreationQuarantineReplayGateway> discoverReplayGateways(
			final ListableBeanFactory beanFactory)
	{
		var discovered = new LinkedHashMap<String, CreationQuarantineReplayGateway>();
		var seen = java.util.Collections.newSetFromMap(new IdentityHashMap<CreationQuarantineReplayGateway, Boolean>());
		for (var gateway : beanFactory.getBeansOfType(CreationQuarantineReplayGateway.class).values())
		{
			registerReplayGateway(discovered, seen, gateway);
		}
		for (var service : beanFactory.getBeansOfType(CreationService.class).values())
		{
			asReplayGateway(service).ifPresent(gateway -> registerReplayGateway(discovered, seen, gateway));
		}
		return discovered;
	}

	private static void registerReplayGateway(
			final Map<String, CreationQuarantineReplayGateway> discovered,
			final java.util.Set<CreationQuarantineReplayGateway> seen,
			final CreationQuarantineReplayGateway gateway)
	{
		if (!seen.add(gateway))
		{
			return;
		}
		var duplicate = discovered.putIfAbsent(gateway.aggregateType(), gateway);
		if (duplicate != null)
		{
			throw new IllegalArgumentException(
					"Duplicate creation quarantine replay gateway for aggregate type " + gateway.aggregateType());
		}
	}

	private static java.util.Optional<CreationQuarantineReplayGateway> asReplayGateway(final Object candidate)
	{
		if (candidate instanceof CreationQuarantineReplayGateway gateway)
		{
			return java.util.Optional.of(gateway);
		}
		if (candidate instanceof Advised advised)
		{
			try
			{
				var target = advised.getTargetSource().getTarget();
				if (target instanceof CreationQuarantineReplayGateway gateway)
				{
					return java.util.Optional.of(gateway);
				}
			}
			catch (final Exception exception)
			{
				throw new IllegalStateException("Failed to inspect creation service replay gateway", exception);
			}
		}
		return java.util.Optional.empty();
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
		CreationQuarantineRepository creationQuarantineRepository(
				final EntityManager entityManager,
				final ObjectMapper objectMapper)
		{
			return JpaCreationQuarantineStore.with(entityManager, objectMapper);
		}

		@Bean
		@ConditionalOnMissingBean
		CreationQuarantinePayloadCodec creationQuarantinePayloadCodec(final ObjectMapper objectMapper)
		{
			return JacksonCreationQuarantinePayloadCodec.with(objectMapper);
		}

		@Bean
		@ConditionalOnMissingBean
		CreationQuarantineService creationQuarantineService(
				final CreationQuarantineRepository repository,
				final CreationQuarantineReplayRegistry replayRegistry,
				final CreationQuarantinePayloadCodec payloadCodec,
				final Clock durableProcessClock)
		{
			return DefaultCreationQuarantineService.with(repository, replayRegistry, payloadCodec, durableProcessClock);
		}

		@Bean
		@ConditionalOnMissingBean
		CreationQuarantineRecorder creationQuarantineRecorder(final CreationQuarantineService creationQuarantineService)
		{
			return creationQuarantineService;
		}

		@Bean
		@ConditionalOnMissingBean
		CreationQuarantineApplicationController creationQuarantineApplicationController(
				final CreationQuarantineService creationQuarantineService,
				final CreationQuarantineAccessPolicy creationQuarantineAccessPolicy)
		{
			return CreationQuarantineApplicationControllers.controller(
					creationQuarantineService,
					creationQuarantineAccessPolicy);
		}

		@Bean
		@ConditionalOnMissingBean
		CreationQuarantineWebMapper creationQuarantineWebMapper()
		{
			return new CreationQuarantineWebMapper();
		}

		@Bean
		@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
		@ConditionalOnProperty(prefix = "clean-crud.creation.quarantine", name = "api-enabled", havingValue = "true")
		@ConditionalOnMissingBean
		DefaultSpringRestCreationQuarantineController springRestCreationQuarantineController(
				final CreationQuarantineApplicationController applicationController,
				final CreationQuarantineWebMapper webMapper)
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
