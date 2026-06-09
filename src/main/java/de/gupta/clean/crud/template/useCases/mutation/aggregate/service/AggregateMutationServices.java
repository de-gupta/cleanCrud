package de.gupta.clean.crud.template.useCases.mutation.aggregate.service;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
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
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.evaluation.SourceAwareMutationPolicy;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

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
		@Override
		public IdentifiedModel<DomainId, DomainModel> mutate(final MutationRequest<DomainId, ?> request)
		{
			assertNoRelationships();
			return engine.execute(
					CrudWorkflowBuilder.writeFlow(() -> applyMutation(request))
					                   .startDurableProcesses(result -> durableProcessStartRequests.apply(
											   result.context()))
					                   .afterTransaction(this::dispatchMutationCompleted)
					                   .build()).updated();
		}

		private MutationDispatch<DomainId, DomainModel> applyMutation(final MutationRequest<DomainId, ?> request)
		{
			var current = definition.fetchPort()
			                        .findById(request.domainId())
			                        .orElseThrow(() -> ResourceNotFoundException.withId(request.domainId()));
			var updatedModel = applyRegisteredHandler(current.model(), request.payload());
			sourceAwareMutationPolicy.validate(request.source(), current.model(), updatedModel);
			var updated = definition.mutationPort().update(request.domainId(), updatedModel);
			return new MutationDispatch<>(
					updated,
					new MutationContext<>(
							request.domainId(),
							request.source(),
							request.family(),
							request.payloadType(),
							request.correlationId(),
							request.causationId(),
							Optional.of(current.model()),
							Optional.of(updated.model())));
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

		private void dispatchMutationCompleted(final MutationDispatch<DomainId, DomainModel> result)
		{
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

		private record MutationDispatch<DomainId, DomainModel>(
				IdentifiedModel<DomainId, DomainModel> updated,
				MutationContext<DomainId, DomainModel> context)
		{
		}
	}
}
