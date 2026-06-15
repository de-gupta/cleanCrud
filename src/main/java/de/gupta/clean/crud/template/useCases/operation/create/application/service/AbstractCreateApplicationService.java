package de.gupta.clean.crud.template.useCases.operation.create.application.service;

import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.EvaluatedCreationAttempt;
import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.PreparedCreationAttempt;
import de.gupta.clean.crud.template.useCases.operation.create.domain.execution.CreateExecutor;
import de.gupta.clean.crud.template.useCases.operation.create.domain.handler.CreationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationContext;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationRequest;
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
	private final CreateExecutor<Payload, DomainCreateModel, DomainModel> createExecutor;
	private final CreationQuarantineRecorder quarantineRecorder;

	@Override
	public CreateOperationResult<DomainModel> create(final CreateOperationRequest<Payload> request)
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
			final CreateOperationRequest<Payload> request)
	{
		var handler = handlerRegistry.resolveHandlerFor(payloadTypeOf(request));
		var context = CreateOperationContext.from(request);
		var plan = handler.createPlan(request);
		return new PreparedCreationAttempt<>(request, handler, context, plan);
	}

	private CreateOperationResult<DomainModel> createAllowedResult(
			final EvaluatedCreationAttempt<Payload, DomainCreateModel> evaluatedAttempt)
	{
		return CreationOperationResults.created(evaluatedAttempt.context(),
				createExecutor.create(evaluatedAttempt.preparedAttempt()),
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
	private Class<Payload> payloadTypeOf(final CreateOperationRequest<Payload> request)
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
	                                           final CreateExecutor<Payload, DomainCreateModel, DomainModel> createExecutor)
	{
		this(handlerRegistry, policyEvaluator, createExecutor, CreationQuarantineRecorder.noop());
	}

	protected AbstractCreateApplicationService(final CreationHandlerRegistry<DomainCreateModel> handlerRegistry,
	                                           final CreationPolicyEvaluator policyEvaluator,
	                                           final CreateExecutor<Payload, DomainCreateModel, DomainModel> createExecutor,
	                                           final CreationQuarantineRecorder quarantineRecorder)
	{
		this.handlerRegistry = Objects.requireNonNull(handlerRegistry, "handlerRegistry");
		this.policyEvaluator = Objects.requireNonNull(policyEvaluator, "policyEvaluator");
		this.createExecutor = Objects.requireNonNull(createExecutor, "createExecutor");
		this.quarantineRecorder = Objects.requireNonNull(quarantineRecorder, "quarantineRecorder");
	}
}