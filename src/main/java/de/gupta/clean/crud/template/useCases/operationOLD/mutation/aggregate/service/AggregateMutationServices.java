package de.gupta.clean.crud.template.useCases.operationOLD.mutation.aggregate.service;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.runtime.AggregateWorkflowRunner;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.AggregateServiceSupportFactory;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.relationship.AggregateDefinitionRelationshipInspector;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.aggregate.policy.AggregateMutationPolicies;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.application.service.QuarantinableMutationService;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.handler.MutationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.model.MutationContext;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.evaluation.SourceAwareMutationPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.quarantine.application.recording.MutationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public enum AggregateMutationServices
{
	;

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	QuarantinableMutationService<DomainId, DomainModel> mutationService(
			final String aggregateKey,
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateWorkflowRunner engine,
			final MutationHandlerRegistry<DomainModel> handlerRegistry,
			final MutationQuarantineRecorder mutationQuarantineRecorder)
	{
		return mutationService(
				aggregateKey,
				definition,
				engine,
				handlerRegistry,
				_ -> List.of(),
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateMutationPolicies.sourceAwarePolicy(definition),
				mutationQuarantineRecorder);
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	QuarantinableMutationService<DomainId, DomainModel> mutationService(
			final String aggregateKey,
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateWorkflowRunner engine,
			final MutationHandlerRegistry<DomainModel> handlerRegistry,
			final Function<MutationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
					durableProcessStartRequests,
			final AggregateDefinitionRelationshipInspector definitionGuard,
			final SourceAwareMutationPolicy<DomainModel> sourceAwareMutationPolicy,
			final MutationQuarantineRecorder mutationQuarantineRecorder)
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
						AggregateServiceSupportFactory.validationSupport()), mutationQuarantineRecorder);
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	QuarantinableMutationService<DomainId, DomainModel> mutationService(
			final String aggregateKey,
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateWorkflowRunner engine,
			final MutationHandlerRegistry<DomainModel> handlerRegistry,
			final AggregateDefinitionRelationshipInspector definitionGuard,
			final SourceAwareMutationPolicy<DomainModel> sourceAwareMutationPolicy,
			final MutationQuarantineRecorder mutationQuarantineRecorder)
	{
		return mutationService(aggregateKey, definition, engine, handlerRegistry, _ -> List.of(), definitionGuard,
				sourceAwareMutationPolicy, mutationQuarantineRecorder);
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	QuarantinableMutationService<DomainId, DomainModel> mutationService(
			final String aggregateKey,
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateWorkflowRunner engine,
			final MutationHandlerRegistry<DomainModel> handlerRegistry,
			final Function<MutationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
					durableProcessStartRequests, final MutationQuarantineRecorder mutationQuarantineRecorder)
	{
		return mutationService(
				aggregateKey,
				definition,
				engine,
				handlerRegistry,
				durableProcessStartRequests,
				AggregateServiceSupportFactory.definitionGuard(),
				AggregateMutationPolicies.sourceAwarePolicy(definition),
				mutationQuarantineRecorder);
	}

}