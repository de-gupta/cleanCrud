package de.gupta.clean.crud.template.useCases.operation.creation.aggregate.service;

import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateDefinitionGuard;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateSaveCoordinator;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateServiceSupportFactory;
import de.gupta.clean.crud.template.useCases.operation.creation.aggregate.policy.AggregateCreationPolicies;
import de.gupta.clean.crud.template.useCases.operation.creation.application.service.CreationService;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.handler.CreationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationContext;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.evaluation.SourceAwareCreationPolicy;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class AggregateCreationServices
{
	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	CreationService<DomainId, DomainModel> creationService(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final CreationHandlerRegistry<DomainModelCreate> handlerRegistry)
	{
		return creationService(
				definition,
				engine,
				handlerRegistry,
				_ -> List.of(),
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.saveCoordinator(),
				AggregateCreationPolicies.sourceAwarePolicy(definition));
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	CreationService<DomainId, DomainModel> creationService(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final CreationHandlerRegistry<DomainModelCreate> handlerRegistry,
			final Function<CreationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
					durableProcessStartRequests)
	{
		return creationService(
				definition,
				engine,
				handlerRegistry,
				durableProcessStartRequests,
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.saveCoordinator(),
				AggregateCreationPolicies.sourceAwarePolicy(definition));
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	CreationService<DomainId, DomainModel> creationService(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final CreationHandlerRegistry<DomainModelCreate> handlerRegistry,
			final Function<CreationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
					durableProcessStartRequests,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateSaveCoordinator saveCoordinator,
			final SourceAwareCreationPolicy<DomainModel> sourceAwareCreationPolicy)
	{
		return new DefaultAggregateCreationService<>(
				definition,
				engine,
				handlerRegistry,
				durableProcessStartRequests,
				definitionGuard,
				saveCoordinator,
				sourceAwareCreationPolicy);
	}

	private AggregateCreationServices()
	{
	}
}
