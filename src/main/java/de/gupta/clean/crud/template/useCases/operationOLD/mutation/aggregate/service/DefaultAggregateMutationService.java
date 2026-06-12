package de.gupta.clean.crud.template.useCases.operationOLD.mutation.aggregate.service;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateDefinitionGuard;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.CrudWorkflowBuilder;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.application.service.AbstractMutationService;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.application.service.QuarantinableMutationService;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.handler.MutationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.handler.RegisteredMutationHandler;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.model.MutationContext;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.model.MutationResult;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.plan.AggregateMutationPlan;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.evaluation.SourceAwareMutationPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.quarantine.application.recording.MutationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service.QuarantineReplayCommand;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service.ReplayOutcome;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.MutationReplayData;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.QuarantineId;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class DefaultAggregateMutationService<
		DomainId,
		DomainModel,
		DomainModelCreate,
		DomainModelUpdatePatch,
		DomainModelResponse>
		extends AbstractMutationService<DomainId, DomainModel>
		implements QuarantinableMutationService<DomainId, DomainModel>
{
	private static final Logger log = LoggerFactory.getLogger(DefaultAggregateMutationService.class);

	private final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
			DomainModelResponse> definition;
	private final AggregateLifecycleEngine engine;
	private final MutationHandlerRegistry<DomainModel> handlerRegistry;
	private final Function<MutationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
			durableProcessStartRequests;
	private final AggregateDefinitionGuard definitionGuard;
	private final SourceAwareMutationPolicy<DomainModel> sourceAwareMutationPolicy;
	private final AggregateMutationCoordinator mutationCoordinator;
	private final String aggregateType;

	public DefaultAggregateMutationService(
			final String aggregateKey,
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition,
			final AggregateLifecycleEngine engine,
			final MutationHandlerRegistry<DomainModel> handlerRegistry,
			final Function<MutationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
					durableProcessStartRequests,
			final AggregateDefinitionGuard definitionGuard,
			final SourceAwareMutationPolicy<DomainModel> sourceAwareMutationPolicy,
			final AggregateMutationCoordinator mutationCoordinator)
	{
		this.aggregateType = Objects.requireNonNull(aggregateKey, "aggregateKey");
		this.definition = definition;
		this.engine = engine;
		this.handlerRegistry = handlerRegistry;
		this.durableProcessStartRequests = durableProcessStartRequests;
		this.definitionGuard = definitionGuard;
		this.sourceAwareMutationPolicy = sourceAwareMutationPolicy;
		this.mutationCoordinator = mutationCoordinator;
	}

	@Override
	public MutationResult<DomainId, DomainModel> mutateWithResult(final MutationRequest<DomainId, ?> request)
	{
		return mutateWithResult(request, Optional.empty());
	}

	public String aggregateType()
	{
		return aggregateType;
	}

	@Override
	public String aggregateKey()
	{
		return aggregateType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ReplayOutcome replay(final QuarantineReplayCommand<MutationReplayData> command)
	{
		var data = command.replayInputs();
		var result = mutateWithResult(
				new MutationRequest<>(
						(DomainId) data.domainId(),
						data.payload(),
						OperationSource.ADMINISTRATIVE_REPLAY,
						command.metadata().family(),
						command.metadata().correlationId(),
						command.metadata().causationId()),
				Optional.of(command.quarantineId()));
		if (result.applied())
		{
			return ReplayOutcome.success();
		}
		return ReplayOutcome.quarantined(result.quarantineRequest().map(r -> r.violations().toString()));
	}

	private MutationResult<DomainId, DomainModel> mutateWithResult(
			final MutationRequest<DomainId, ?> request,
			final Optional<QuarantineId> replayQuarantineId)
	{
		return engine.execute(
				CrudWorkflowBuilder.writeFlow(() -> applyMutation(request, replayQuarantineId))
				                   .startDurableProcesses(result -> result.updated()
				                                                          .map(_ -> durableProcessStartRequests.apply(
																				  result.context()))
				                                                          .orElse(List.of()))
				                   .afterTransaction(this::dispatchMutationCompleted)
				                   .build());
	}

	private MutationResult<DomainId, DomainModel> applyMutation(
			final MutationRequest<DomainId, ?> request,
			final Optional<QuarantineId> replayQuarantineId)
	{
		var relationships = definitionGuard.satelliteRelationships(definition);
		var current = definition.fetchPort()
		                        .findById(request.domainId())
		                        .orElseThrow(() -> ResourceNotFoundException.withId(request.domainId()));
		var mutationPlan = applyRegisteredHandler(current.model(), request.payload());
		var updatedModel = applyMutationPlan(relationships, current.model(), mutationPlan, request);
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
			if (replayQuarantineId.isEmpty())
			{
				policyDecision = persistQuarantine(request, policyDecision);
			}
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

	private DomainModel applyMutationPlan(
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					?, ?, ?, ?>> relationships,
			final DomainModel currentModel,
			final AggregateMutationPlan<DomainModel> mutationPlan,
			final MutationRequest<DomainId, ?> request)
	{
		if (relationships.isEmpty())
		{
			return mutationPlan.updatedRoot().orElse(currentModel);
		}
		return mutationCoordinator.applyPlan(definition, relationships, currentModel, mutationPlan, request.source());
	}

	private AggregateMutationPlan<DomainModel> applyRegisteredHandler(
			final DomainModel currentModel,
			final ApplicationOperationPayload payload)
	{
		var registeredHandler = handlerRegistry.findHandlerFor(payload.getClass())
		                                       .orElseThrow(() -> InvalidRequestException.withMessage(
													   "No mutation handler registered for payload type "
															   + payload.getClass().getName()));
		return applyTypedHandler(registeredHandler, currentModel, payload);
	}

	@SuppressWarnings("unchecked")
	private AggregateMutationPlan<DomainModel> applyTypedHandler(
			final RegisteredMutationHandler<DomainModel, ?> registeredHandler,
			final DomainModel currentModel,
			final ApplicationOperationPayload payload)
	{
		return ((de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.handler.AggregateMutationHandler<DomainModel, ApplicationOperationPayload>) registeredHandler.handler()).apply(
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

	private de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.evaluation.MutationPolicyDecision persistQuarantine(
			final MutationRequest<DomainId, ?> request,
			final de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.evaluation.MutationPolicyDecision policyDecision)
	{
		var persistedRequest = engine.mutationQuarantineRecorder().record(new MutationQuarantineSubmission(
				aggregateType,
				request,
				policyDecision.quarantineRequest().orElseThrow()));
		return policyDecision.withQuarantineRequest(persistedRequest);
	}
}