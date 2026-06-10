package de.gupta.clean.crud.template.useCases.incantation.aggregate.service;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateDefinitionGuard;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.CrudWorkflowBuilder;
import de.gupta.clean.crud.template.useCases.incantation.application.service.AbstractIncantationService;
import de.gupta.clean.crud.template.useCases.incantation.domain.handler.AggregateIncantationHandler;
import de.gupta.clean.crud.template.useCases.incantation.domain.handler.IncantationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.incantation.domain.handler.RegisteredIncantationHandler;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.*;
import de.gupta.clean.crud.template.useCases.incantation.domain.plan.AggregateIncantationPlan;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.evaluation.SourceAwareIncantationPolicy;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class DefaultAggregateIncantationService<
		DomainId,
		DomainModel,
		DomainModelCreate,
		DomainModelUpdatePatch,
		DomainModelResponse>
		extends AbstractIncantationService<DomainId, DomainModel>
{
	private final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
			DomainModelResponse> definition;
	private final AggregateLifecycleEngine engine;
	private final IncantationHandlerRegistry<DomainModelCreate> handlerRegistry;
	private final Function<IncantationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
			durableProcessStartRequests;
	private final AggregateDefinitionGuard definitionGuard;
	private final SourceAwareIncantationPolicy<DomainModel> sourceAwareIncantationPolicy;

	public DefaultAggregateIncantationService(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final IncantationHandlerRegistry<DomainModelCreate> handlerRegistry,
			final Function<IncantationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
					durableProcessStartRequests,
			final AggregateDefinitionGuard definitionGuard,
			final SourceAwareIncantationPolicy<DomainModel> sourceAwareIncantationPolicy)
	{
		this.definition = definition;
		this.engine = engine;
		this.handlerRegistry = handlerRegistry;
		this.durableProcessStartRequests = durableProcessStartRequests;
		this.definitionGuard = definitionGuard;
		this.sourceAwareIncantationPolicy = sourceAwareIncantationPolicy;
	}

	@Override
	public IncantationResult<DomainId, DomainModel> incantWithResult(final IncantationRequest<?> request)
	{
		return engine.execute(
				CrudWorkflowBuilder.writeFlow(() -> applyIncantation(request))
				                   .startDurableProcesses(result -> result.created()
				                                                          .map(_ -> durableProcessStartRequests.apply(
																				  result.context()))
				                                                          .orElse(List.of()))
				                   .afterTransaction(this::dispatchIncantationCompleted)
				                   .build());
	}

	private IncantationResult<DomainId, DomainModel> applyIncantation(final IncantationRequest<?> request)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		if (!relationships.isEmpty())
		{
			throw InvalidRequestException.withMessage(
					"Incantation create currently supports only root-only aggregates; relationship support is deferred");
		}

		var plan = applyRegisteredHandler(request.payload());
		var createInput = plan.rootCreate().orElseThrow(() -> InvalidRequestException.withMessage(
				"Incantation plan did not provide root create input"));
		var candidateModel = definition.createBuilder().toModel(createInput);
		var policyDecision = sourceAwareIncantationPolicy.evaluate(request.source(), candidateModel);
		var baseContext = new IncantationContext<DomainId, DomainModel>(
				Optional.empty(),
				request.source(),
				request.family(),
				request.payloadType(),
				request.correlationId(),
				request.causationId(),
				Optional.empty(),
				Optional.of(candidateModel));
		if (policyDecision.quarantined())
		{
			return IncantationResult.quarantined(baseContext, policyDecision);
		}

		var created = definition.mutationPort().create(candidateModel);
		var context = baseContext.withCreated(created.id(), created.model());
		return IncantationResult.created(context, policyDecision, new CreateResult<>(created));
	}

	private void dispatchIncantationCompleted(final IncantationResult<DomainId, DomainModel> result)
	{
		if (!result.creationApplied())
		{
			return;
		}
		definition.postCommitMutation().accept(new PostCommitMutationContext<>(
				PostCommitMutationKind.CREATE,
				result.context().domainId().orElseThrow(),
				result.context().afterModel(),
				Optional.empty()));
	}

	private AggregateIncantationPlan<DomainModelCreate> applyRegisteredHandler(
			final ApplicationIncantationPayload payload)
	{
		var registeredHandler = handlerRegistry.findHandlerFor(payload.getClass())
		                                       .orElseThrow(() -> InvalidRequestException.withMessage(
													   "No incantation handler registered for payload type "
															   + payload.getClass().getName()));
		return applyTypedHandler(registeredHandler, payload);
	}

	@SuppressWarnings("unchecked")
	private AggregateIncantationPlan<DomainModelCreate> applyTypedHandler(
			final RegisteredIncantationHandler<DomainModelCreate, ?> registeredHandler,
			final ApplicationIncantationPayload payload)
	{
		return ((AggregateIncantationHandler<DomainModelCreate, ApplicationIncantationPayload>) registeredHandler.handler()).apply(
				payload);
	}
}
