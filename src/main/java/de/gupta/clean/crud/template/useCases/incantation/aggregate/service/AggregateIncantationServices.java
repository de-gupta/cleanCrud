package de.gupta.clean.crud.template.useCases.incantation.aggregate.service;

import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateDefinitionGuard;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateServiceSupportFactory;
import de.gupta.clean.crud.template.useCases.incantation.aggregate.policy.AggregateIncantationPolicies;
import de.gupta.clean.crud.template.useCases.incantation.application.service.IncantationService;
import de.gupta.clean.crud.template.useCases.incantation.domain.handler.IncantationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationContext;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.evaluation.SourceAwareIncantationPolicy;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class AggregateIncantationServices
{
	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	IncantationService<DomainId, DomainModel> incantationService(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final IncantationHandlerRegistry<DomainModelCreate> handlerRegistry)
	{
		return incantationService(
				definition,
				engine,
				handlerRegistry,
				_ -> List.of(),
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateIncantationPolicies.sourceAwarePolicy(definition));
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	IncantationService<DomainId, DomainModel> incantationService(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final IncantationHandlerRegistry<DomainModelCreate> handlerRegistry,
			final Function<IncantationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
					durableProcessStartRequests)
	{
		return incantationService(
				definition,
				engine,
				handlerRegistry,
				durableProcessStartRequests,
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateIncantationPolicies.sourceAwarePolicy(definition));
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	IncantationService<DomainId, DomainModel> incantationService(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final IncantationHandlerRegistry<DomainModelCreate> handlerRegistry,
			final Function<IncantationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
					durableProcessStartRequests,
			final AggregateDefinitionGuard definitionGuard,
			final SourceAwareIncantationPolicy<DomainModel> sourceAwareIncantationPolicy)
	{
		return new DefaultAggregateIncantationService<>(
				definition,
				engine,
				handlerRegistry,
				durableProcessStartRequests,
				definitionGuard,
				sourceAwareIncantationPolicy);
	}

	private AggregateIncantationServices()
	{
	}
}
