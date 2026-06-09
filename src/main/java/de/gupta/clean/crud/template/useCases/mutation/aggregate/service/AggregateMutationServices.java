package de.gupta.clean.crud.template.useCases.mutation.aggregate.service;

import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateDefinitionGuard;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateServiceSupportFactory;
import de.gupta.clean.crud.template.useCases.mutation.aggregate.policy.AggregateMutationPolicies;
import de.gupta.clean.crud.template.useCases.mutation.application.service.MutationService;
import de.gupta.clean.crud.template.useCases.mutation.domain.handler.MutationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationContext;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.evaluation.SourceAwareMutationPolicy;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class AggregateMutationServices
{
	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	MutationService<DomainId, DomainModel> mutationService(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final MutationHandlerRegistry<DomainModel> handlerRegistry)
	{
		return mutationService(
				definition,
				engine,
				handlerRegistry,
				_ -> List.of(),
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateMutationPolicies.sourceAwarePolicy(definition));
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	MutationService<DomainId, DomainModel> mutationService(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final MutationHandlerRegistry<DomainModel> handlerRegistry,
			final Function<MutationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
					durableProcessStartRequests)
	{
		return mutationService(
				definition,
				engine,
				handlerRegistry,
				durableProcessStartRequests,
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateMutationPolicies.sourceAwarePolicy(definition));
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	MutationService<DomainId, DomainModel> mutationService(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final MutationHandlerRegistry<DomainModel> handlerRegistry,
			final AggregateDefinitionGuard definitionGuard,
			final SourceAwareMutationPolicy<DomainModel> sourceAwareMutationPolicy)
	{
		return mutationService(definition, engine, handlerRegistry, _ -> List.of(), definitionGuard,
				sourceAwareMutationPolicy);
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	MutationService<DomainId, DomainModel> mutationService(
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
				definition,
				engine,
				handlerRegistry,
				durableProcessStartRequests,
				definitionGuard,
				sourceAwareMutationPolicy);
	}

	private AggregateMutationServices()
	{
	}
}
