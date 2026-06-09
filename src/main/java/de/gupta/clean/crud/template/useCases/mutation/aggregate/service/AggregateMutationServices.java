package de.gupta.clean.crud.template.useCases.mutation.aggregate.service;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.*;
import de.gupta.clean.crud.template.useCases.mutation.aggregate.policy.AggregateMutationPolicies;
import de.gupta.clean.crud.template.useCases.mutation.application.service.MutationService;
import de.gupta.clean.crud.template.useCases.mutation.domain.handler.MutationHandler;
import de.gupta.clean.crud.template.useCases.mutation.domain.handler.MutationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.mutation.domain.handler.RegisteredMutationHandler;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.ApplicationMutationPayload;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationContext;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationResult;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.evaluation.SourceAwareMutationPolicy;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
		return new AggregateMutationService<>(definition, engine, handlerRegistry, durableProcessStartRequests,
				definitionGuard, sourceAwareMutationPolicy);
	}

	private AggregateMutationServices()
	{
	}

	private record AggregateMutationService<
			DomainId,
			DomainModel,
			DomainModelCreate,
			DomainModelUpdatePatch,
			DomainModelResponse>(
			AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse> definition,
			AggregateLifecycleEngine engine, MutationHandlerRegistry<DomainModel> handlerRegistry,
			Function<MutationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests,
			AggregateDefinitionGuard definitionGuard,
			SourceAwareMutationPolicy<DomainModel> sourceAwareMutationPolicy)
			implements MutationService<DomainId, DomainModel>
	{
		private static final Logger log = LoggerFactory.getLogger(AggregateMutationService.class);

		@Override
		public MutationResult<DomainId, DomainModel> mutateWithResult(final MutationRequest<DomainId, ?> request)
		{
			assertNoRelationships();
			return engine.execute(
					CrudWorkflowBuilder.writeFlow(() -> applyMutation(request))
					                   .startDurableProcesses(result -> result.updated()
					                                                          .map(_ -> durableProcessStartRequests.apply(
																					  result.context()))
					                                                          .orElse(List.of()))
					                   .afterTransaction(this::dispatchMutationCompleted)
					                   .build());
		}

		private MutationResult<DomainId, DomainModel> applyMutation(final MutationRequest<DomainId, ?> request)
		{
			var current = definition.fetchPort()
			                        .findById(request.domainId())
			                        .orElseThrow(() -> ResourceNotFoundException.withId(request.domainId()));
			var updatedModel = applyRegisteredHandler(current.model(), request.payload());
			var policyDecision = sourceAwareMutationPolicy.evaluate(request.source(), current.model(), updatedModel);
			var context = new MutationContext<>(
					request.domainId(),
					request.source(),
					request.family(),
					request.payloadType(),
					request.correlationId(),
					request.causationId(),
					Optional.of(current.model()),
					Optional.of(updatedModel));
			if (policyDecision.quarantined())
			{
				log.warn(
						"Mutation quarantined for id {} from source {} with violations {}",
						request.domainId(),
						request.source(),
						policyDecision.quarantineRequest().orElseThrow().violations());
				return MutationResult.quarantined(context, policyDecision);
			}
			var updated = definition.mutationPort().update(request.domainId(), updatedModel);
			return MutationResult.applied(context, policyDecision, updated);
		}

		private DomainModel applyRegisteredHandler(
				final DomainModel currentModel,
				final ApplicationMutationPayload payload)
		{
			var registeredHandler = handlerRegistry.findHandlerFor(payload.getClass())
			                                       .orElseThrow(() -> InvalidRequestException.withMessage(
														   "No mutation handler registered for payload type "
																   + payload.getClass().getName()));
			return applyTypedHandler(registeredHandler, currentModel, payload);
		}

		@SuppressWarnings("unchecked")
		private DomainModel applyTypedHandler(
				final RegisteredMutationHandler<DomainModel, ?> registeredHandler,
				final DomainModel currentModel,
				final ApplicationMutationPayload payload)
		{
			return ((MutationHandler<DomainModel, ApplicationMutationPayload>) registeredHandler.handler()).apply(
					currentModel,
					payload);
		}

		private void dispatchMutationCompleted(final MutationResult<DomainId, DomainModel> result)
		{
			if (result.quarantined())
			{
				return;
			}
			definition.postCommitMutation().accept(new PostCommitMutationContext<>(
					PostCommitMutationKind.PATCH,
					result.context().domainId(),
					result.context().afterModel(),
					result.context().beforeModel()));
		}

		private void assertNoRelationships()
		{
			if (!definitionGuard.satelliteRelationships(definition).isEmpty())
			{
				throw AggregateRelationshipExecutionNotSupportedException.withMessage(
						"Aggregate mutation service currently supports only aggregates without relationships");
			}
		}
	}
}