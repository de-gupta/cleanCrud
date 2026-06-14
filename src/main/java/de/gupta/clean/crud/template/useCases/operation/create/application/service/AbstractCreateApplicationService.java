package de.gupta.clean.crud.template.useCases.operation.create.application.service;

import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.EvaluatedCreationAttempt;
import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.PreparedCreationAttempt;
import de.gupta.clean.crud.template.useCases.operation.create.domain.execution.CreationExecutor;
import de.gupta.clean.crud.template.useCases.operation.create.domain.handler.CreationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationContext;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.policy.CreationPolicyEvaluator;
import de.gupta.clean.crud.template.useCases.operation.create.domain.quarantine.CreationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreateOperationResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationResults;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractCreateApplicationService<Payload extends CreateOperationPayload, DomainCreateModel, DomainModel>
		implements CreateApplicationService<Payload, DomainModel>
{
	private final CreationHandlerRegistry<DomainCreateModel> handlerRegistry;
	private final CreationPolicyEvaluator policyEvaluator;
	private final CreationExecutor<Payload, DomainCreateModel, DomainModel> creationExecutor;
	private final CreationQuarantineRecorder quarantineRecorder;

	@Override
	public CreateOperationResult<DomainModel> create(final CreationOperationRequest<Payload> request)
	{
		return resultFor(evaluateAttempt(prepareAttempt(request)));
	}

	private CreateOperationResult<DomainModel> resultFor(
			final EvaluatedCreationAttempt<Payload, DomainCreateModel> evaluatedAttempt)
	{
		return switch (evaluatedAttempt.decision())
		{
			case ALLOW -> createAllowedResult(evaluatedAttempt);
			case REJECT -> createRejectedResult(evaluatedAttempt);
			case QUARANTINE -> createQuarantinedResult(evaluatedAttempt);
		};
	}

	private EvaluatedCreationAttempt<Payload, DomainCreateModel> evaluateAttempt(
			final PreparedCreationAttempt<Payload, DomainCreateModel> preparedAttempt)
	{
		return EvaluatedCreationAttempt.of(preparedAttempt, policyEvaluator.evaluate(preparedAttempt));
	}

	private PreparedCreationAttempt<Payload, DomainCreateModel> prepareAttempt(
			final CreationOperationRequest<Payload> request)
	{
		var handler = handlerRegistry.resolveHandlerFor(payloadTypeOf(request));
		var context = CreationOperationContext.from(request);
		var plan = handler.createPlan(request);
		return new PreparedCreationAttempt<>(request, handler, context, plan);
	}

	private CreateOperationResult<DomainModel> createAllowedResult(
			final EvaluatedCreationAttempt<Payload, DomainCreateModel> evaluatedAttempt)
	{
		return CreationOperationResults.created(evaluatedAttempt.context(),
				creationExecutor.create(evaluatedAttempt.preparedAttempt()),
				List.copyOf(evaluatedAttempt.toleratedViolations()));
	}

	private CreateOperationResult<DomainModel> createRejectedResult(
			final EvaluatedCreationAttempt<Payload, DomainCreateModel> evaluatedAttempt)
	{
		return CreationOperationResults.rejected(evaluatedAttempt.context(),
				List.copyOf(evaluatedAttempt.blockingViolations()),
				List.copyOf(evaluatedAttempt.toleratedViolations()));
	}

	private CreateOperationResult<DomainModel> createQuarantinedResult(
			final EvaluatedCreationAttempt<Payload, DomainCreateModel> evaluatedAttempt)
	{
		return CreationOperationResults.quarantined(evaluatedAttempt.context(),
				List.copyOf(evaluatedAttempt.blockingViolations()), List.copyOf(evaluatedAttempt.toleratedViolations()),
				recordQuarantine(evaluatedAttempt));
	}

	@SuppressWarnings("unchecked")
	private Class<Payload> payloadTypeOf(final CreationOperationRequest<Payload> request)
	{
		return (Class<Payload>) request.payload().getClass();
	}

	private Optional<String> recordQuarantine(
			final EvaluatedCreationAttempt<Payload, DomainCreateModel> evaluatedAttempt)
	{
		return evaluatedAttempt.quarantineReference().or(() -> quarantineRecorder.record(evaluatedAttempt));
	}

	protected AbstractCreateApplicationService(final CreationHandlerRegistry<DomainCreateModel> handlerRegistry,
	                                           final CreationPolicyEvaluator policyEvaluator,
	                                           final CreationExecutor<Payload, DomainCreateModel, DomainModel> creationExecutor)
	{
		this(handlerRegistry, policyEvaluator, creationExecutor, CreationQuarantineRecorder.noop());
	}

	protected AbstractCreateApplicationService(final CreationHandlerRegistry<DomainCreateModel> handlerRegistry,
	                                           final CreationPolicyEvaluator policyEvaluator,
	                                           final CreationExecutor<Payload, DomainCreateModel, DomainModel> creationExecutor,
	                                           final CreationQuarantineRecorder quarantineRecorder)
	{
		this.handlerRegistry = Objects.requireNonNull(handlerRegistry, "handlerRegistry");
		this.policyEvaluator = Objects.requireNonNull(policyEvaluator, "policyEvaluator");
		this.creationExecutor = Objects.requireNonNull(creationExecutor, "creationExecutor");
		this.quarantineRecorder = Objects.requireNonNull(quarantineRecorder, "quarantineRecorder");
	}
}