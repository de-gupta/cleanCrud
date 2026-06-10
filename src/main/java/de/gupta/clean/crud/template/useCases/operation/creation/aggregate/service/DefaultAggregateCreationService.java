package de.gupta.clean.crud.template.useCases.operation.creation.aggregate.service;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.*;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.useCases.operation.creation.aggregate.policy.AggregateCreationPolicies;
import de.gupta.clean.crud.template.useCases.operation.creation.application.service.AbstractCreationService;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.handler.AggregateCreationHandler;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.handler.CreationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.handler.RegisteredCreationHandler;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreateResult;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationContext;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationResult;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.plan.AggregateCreationPlan;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.evaluation.SourceAwareCreationPolicy;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine.QuarantinedCreationException;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.CreationQuarantineReplayCommand;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.CreationQuarantineReplayGateway;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.recording.CreationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class DefaultAggregateCreationService<
		DomainId,
		DomainModel,
		DomainModelCreate,
		DomainModelUpdatePatch,
		DomainModelResponse>
		extends AbstractCreationService<DomainId, DomainModel>
		implements CreationQuarantineReplayGateway
{
	private final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
			DomainModelResponse> definition;
	private final AggregateLifecycleEngine engine;
	private final CreationHandlerRegistry<DomainModelCreate> handlerRegistry;
	private final Function<CreationContext<DomainId, DomainModel>, Collection<DurableProcessStartRequest<?, ?>>>
			durableProcessStartRequests;
	private final AggregateDefinitionGuard definitionGuard;
	private final AggregateSaveCoordinator saveCoordinator;
	private final SourceAwareCreationPolicy<DomainModel> sourceAwareCreationPolicy;
	private final String aggregateType;

	public DefaultAggregateCreationService(
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
		this.aggregateType = Objects.requireNonNull(aggregateKey, "aggregateKey");
		this.definition = definition;
		this.engine = engine;
		this.handlerRegistry = handlerRegistry;
		this.durableProcessStartRequests = durableProcessStartRequests;
		this.definitionGuard = definitionGuard;
		this.saveCoordinator = saveCoordinator;
		this.sourceAwareCreationPolicy = sourceAwareCreationPolicy;
	}

	@Override
	public CreationResult<DomainId, DomainModel> createWithResult(final CreationRequest<?> request)
	{
		return createWithResult(request, Optional.empty());
	}

	@Override
	public String aggregateType()
	{
		return aggregateType;
	}

	@Override
	public CreationResult<?, ?> replay(final CreationQuarantineReplayCommand command)
	{
		return createWithResult(
				new CreationRequest<>(
						command.payload(),
						OperationSource.ADMINISTRATIVE_REPLAY,
						command.family(),
						command.correlationId(),
						command.causationId()),
				Optional.of(command.quarantineId()));
	}

	private CreationResult<DomainId, DomainModel> createWithResult(
			final CreationRequest<?> request,
			final Optional<CreationQuarantineId> replayQuarantineId)
	{
		try
		{
			return engine.execute(
					CrudWorkflowBuilder.writeFlow(() -> applyCreation(request, replayQuarantineId))
					                   .startDurableProcesses(result -> result.created()
					                                                          .map(_ -> durableProcessStartRequests.apply(
																					  result.context()))
					                                                          .orElse(List.of()))
					                   .afterTransaction(this::dispatchCreationCompleted)
					                   .build());
		}
		catch (final QuarantinedAggregateCreationResultException exception)
		{
			return exception.result();
		}
	}

	private CreationResult<DomainId, DomainModel> applyCreation(
			final CreationRequest<?> request,
			final Optional<CreationQuarantineId> replayQuarantineId)
	{
		var plan = applyRegisteredHandler(request.payload());
		var createInput = plan.rootCreate().orElseThrow(() -> InvalidRequestException.withMessage(
				"Creation plan did not provide root create input"));
		var candidateModel = definition.createBuilder().toModel(createInput);
		var relationships = definitionGuard.satelliteRelationships(definition);
		var policyDecision = sourceAwareCreationPolicy.evaluate(request.source(), candidateModel);
		var baseContext = new CreationContext<DomainId, DomainModel>(
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
			if (replayQuarantineId.isEmpty())
			{
				policyDecision = persistQuarantine(request, policyDecision);
			}
			return CreationResult.quarantined(baseContext, policyDecision);
		}

		var created = relationships.isEmpty()
				? definition.mutationPort().create(candidateModel)
				: createAggregateWithRelationships(
				request,
				relationships,
				createInput,
				policyDecision,
				baseContext,
				replayQuarantineId);
		var context = baseContext.withCreated(created.id(), created.model());
		return CreationResult.created(context, policyDecision, CreateResult.of(created.id(), created.model()));
	}

	private IdentifiedModel<DomainId, DomainModel> createAggregateWithRelationships(
			final CreationRequest<?> request,
			final List<AggregateRelationshipDefinition<DomainId, DomainModel, DomainModelCreate,
					DomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final DomainModelCreate createInput,
			final de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.evaluation.CreationPolicyDecision policyDecision,
			final CreationContext<DomainId, DomainModel> baseContext,
			final Optional<CreationQuarantineId> replayQuarantineId)
	{
		var satelliteCreateIntentResolver = AggregateServiceSupportFactory.satelliteCreateIntentResolver(
				AggregateCreationPolicies.satelliteCreateValidator(request.source()));
		try
		{
			return saveCoordinator.saveAll(
										  definition,
										  relationships,
										  List.of(createInput),
										  domainModel -> sourceAwareCreationPolicy.validate(request.source(), domainModel),
										  satelliteCreateIntentResolver)
			                      .stream()
			                      .findFirst()
			                      .orElseThrow();
		}
		catch (final QuarantinedCreationException exception)
		{
			throw quarantinedAggregateCreation(exception, request, baseContext, policyDecision, replayQuarantineId);
		}
	}

	private RuntimeException quarantinedAggregateCreation(
			final QuarantinedCreationException exception,
			final CreationRequest<?> request,
			final CreationContext<DomainId, DomainModel> baseContext,
			final de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.evaluation.CreationPolicyDecision policyDecision,
			final Optional<CreationQuarantineId> replayQuarantineId)
	{
		var quarantineDecision =
				de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.evaluation.CreationPolicyDecision.quarantine(
						exception.request(),
						policyDecision.toleratedViolations());
		if (replayQuarantineId.isEmpty())
		{
			quarantineDecision = persistQuarantine(request, quarantineDecision);
		}
		return new QuarantinedAggregateCreationResultException(CreationResult.quarantined(
				baseContext,
				quarantineDecision));
	}

	private void dispatchCreationCompleted(final CreationResult<DomainId, DomainModel> result)
	{
		if (!result.applied())
		{
			return;
		}
		definition.postCommitMutation().accept(new PostCommitMutationContext<>(
				PostCommitMutationKind.CREATE,
				result.context().domainId().orElseThrow(),
				result.context().afterModel(),
				Optional.empty()));
	}

	private AggregateCreationPlan<DomainModelCreate> applyRegisteredHandler(
			final ApplicationOperationPayload payload)
	{
		var registeredHandler = handlerRegistry.findHandlerFor(payload.getClass())
		                                       .orElseThrow(() -> InvalidRequestException.withMessage(
													   "No creation handler registered for payload type "
															   + payload.getClass().getName()));
		return applyTypedHandler(registeredHandler, payload);
	}

	@SuppressWarnings("unchecked")
	private AggregateCreationPlan<DomainModelCreate> applyTypedHandler(
			final RegisteredCreationHandler<DomainModelCreate, ?> registeredHandler,
			final ApplicationOperationPayload payload)
	{
		return ((AggregateCreationHandler<DomainModelCreate, ApplicationOperationPayload>) registeredHandler.handler()).apply(
				payload);
	}

	private de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.evaluation.CreationPolicyDecision persistQuarantine(
			final CreationRequest<?> request,
			final de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.evaluation.CreationPolicyDecision policyDecision)
	{
		var persistedRequest = engine.creationQuarantineRecorder().record(new CreationQuarantineSubmission(
				aggregateType,
				request,
				policyDecision.quarantineRequest().orElseThrow()));
		return policyDecision.withQuarantineRequest(persistedRequest);
	}

	private static final class QuarantinedAggregateCreationResultException extends RuntimeException
	{
		private final CreationResult<?, ?> result;

		@SuppressWarnings("unchecked")
		private <DomainId, DomainModel> CreationResult<DomainId, DomainModel> result()
		{
			return (CreationResult<DomainId, DomainModel>) result;
		}

		private QuarantinedAggregateCreationResultException(
				final CreationResult<?, ?> result)
		{
			this.result = result;
		}
	}
}