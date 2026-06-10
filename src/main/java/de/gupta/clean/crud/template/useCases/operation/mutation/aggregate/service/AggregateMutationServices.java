package de.gupta.clean.crud.template.useCases.operation.mutation.aggregate.service;

import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateDefinitionGuard;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateServiceSupportFactory;
import de.gupta.clean.crud.template.useCases.operation.mutation.aggregate.policy.AggregateMutationPolicies;
import de.gupta.clean.crud.template.useCases.operation.mutation.application.service.MutationService;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.handler.MutationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationContext;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.evaluation.SourceAwareMutationPolicy;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class AggregateMutationServices
{
	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	MutationService<DomainId, DomainModel> mutationService(
			final String aggregateKey,
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final MutationHandlerRegistry<DomainModel> handlerRegistry)
	{
		return mutationService(
				aggregateKey,
				definition,
				engine,
				handlerRegistry,
				_ -> List.of(),
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateMutationPolicies.sourceAwarePolicy(definition));
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	MutationService<DomainId, DomainModel> mutationService(
			final String aggregateKey,
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final MutationHandlerRegistry<DomainModel> handlerRegistry,
			final Function<MutationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
					durableProcessStartRequests)
	{
		return mutationService(
				aggregateKey,
				definition,
				engine,
				handlerRegistry,
				durableProcessStartRequests,
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateMutationPolicies.sourceAwarePolicy(definition));
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	MutationService<DomainId, DomainModel> mutationService(
			final String aggregateKey,
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final MutationHandlerRegistry<DomainModel> handlerRegistry,
			final AggregateDefinitionGuard definitionGuard,
			final SourceAwareMutationPolicy<DomainModel> sourceAwareMutationPolicy)
	{
		return mutationService(aggregateKey, definition, engine, handlerRegistry, _ -> List.of(), definitionGuard,
				sourceAwareMutationPolicy);
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	MutationService<DomainId, DomainModel> mutationService(
			final String aggregateKey,
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final MutationHandlerRegistry<DomainModel> handlerRegistry,
			final Function<MutationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
					durableProcessStartRequests,
			final AggregateDefinitionGuard definitionGuard,
			final SourceAwareMutationPolicy<DomainModel> sourceAwareMutationPolicy)
	{
		return new DefaultAggregateMutationService<>(
				aggregateKey,
				definition,
				engine,
				handlerRegistry,
				durableProcessStartRequests,
				definitionGuard,
				sourceAwareMutationPolicy,
				AggregateMutationCoordinator.with(
						AggregateServiceSupportFactory.relationshipPlanner(),
						AggregateServiceSupportFactory.referenceResolver(),
						AggregateServiceSupportFactory.validationSupport()));
	}

	private AggregateMutationServices()
	{
	}
}
