package de.gupta.clean.crud.template.useCases.operationOLD.creation.aggregate.service;

import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateDefinitionGuard;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateSaveCoordinator;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateServiceSupportFactory;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.aggregate.policy.AggregateCreationPolicies;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.application.service.QuarantinableCreationService;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.handler.CreationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.model.CreationContext;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.evaluation.SourceAwareCreationPolicy;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class AggregateCreationServices
{
	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	QuarantinableCreationService<DomainId, DomainModel> creationService(
			final String aggregateKey,
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final CreationHandlerRegistry<DomainModelCreate> handlerRegistry)
	{
		return creationService(
				aggregateKey,
				definition,
				engine,
				handlerRegistry,
				_ -> List.of(),
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.saveCoordinator(),
				AggregateCreationPolicies.sourceAwarePolicy(definition));
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	QuarantinableCreationService<DomainId, DomainModel> creationService(
			final String aggregateKey,
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final CreationHandlerRegistry<DomainModelCreate> handlerRegistry,
			final Function<CreationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
					durableProcessStartRequests)
	{
		return creationService(
				aggregateKey,
				definition,
				engine,
				handlerRegistry,
				durableProcessStartRequests,
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateServiceSupportFactory.saveCoordinator(),
				AggregateCreationPolicies.sourceAwarePolicy(definition));
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	QuarantinableCreationService<DomainId, DomainModel> creationService(
			final String aggregateKey,
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final CreationHandlerRegistry<DomainModelCreate> handlerRegistry,
			final AggregateDefinitionGuard definitionGuard,
			final AggregateSaveCoordinator saveCoordinator,
			final SourceAwareCreationPolicy<DomainModel> sourceAwareCreationPolicy)
	{
		return creationService(aggregateKey, definition, engine, handlerRegistry, _ -> List.of(), definitionGuard,
				saveCoordinator, sourceAwareCreationPolicy);
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	QuarantinableCreationService<DomainId, DomainModel> creationService(
			final String aggregateKey,
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
				aggregateKey,
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