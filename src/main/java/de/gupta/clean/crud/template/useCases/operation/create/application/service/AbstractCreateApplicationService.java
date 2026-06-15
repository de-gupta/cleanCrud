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

public abstract class AbstractCreateApplicationService<DomainCreatePayload extends CreateOperationPayload, DomainModel>
		implements CreateApplicationService<DomainCreatePayload, DomainModel>
{
	private final CreationHandlerRegistry<DomainModel> handlerRegistry;
	private final CreationPolicyEvaluator policyEvaluator;
	private final CreateExecutor<DomainCreatePayload, DomainModel> createExecutor;
	private final CreationQuarantineRecorder quarantineRecorder;

	@Override
	public CreateOperationResult<DomainModel> create(final CreateOperationRequest<DomainCreatePayload> request)
	{
		return resultFor(evaluateAttempt(prepareAttempt(request)));
	}

	private CreateOperationResult<DomainModel> resultFor(
			final EvaluatedCreationAttempt<DomainCreatePayload, DomainModel> evaluatedAttempt)
	{
		return switch (evaluatedAttempt.decision())
		{
			case ALLOW -> createAllowedResult(evaluatedAttempt);
			case REJECT -> createRejectedResult(evaluatedAttempt);
			case QUARANTINE -> createQuarantinedResult(evaluatedAttempt);
		};
	}

	private EvaluatedCreationAttempt<DomainCreatePayload, DomainModel> evaluateAttempt(
			final PreparedCreationAttempt<DomainCreatePayload, DomainModel> preparedAttempt)
	{
		return EvaluatedCreationAttempt.of(preparedAttempt, policyEvaluator.evaluate(preparedAttempt));
	}

	private PreparedCreationAttempt<DomainCreatePayload, DomainModel> prepareAttempt(
			final CreateOperationRequest<DomainCreatePayload> request)
	{
		var handler = handlerRegistry.resolveHandlerFor(payloadTypeOf(request));
		var context = CreateOperationContext.from(request);
		var plan = handler.createPlan(request);
		return new PreparedCreationAttempt<>(request, handler, context, plan);
	}

	private CreateOperationResult<DomainModel> createAllowedResult(
			final EvaluatedCreationAttempt<DomainCreatePayload, DomainModel> evaluatedAttempt)
	{
		return CreationOperationResults.created(evaluatedAttempt.context(),
				createExecutor.create(evaluatedAttempt.preparedAttempt()),
				List.copyOf(evaluatedAttempt.toleratedViolations()));
	}

	private CreateOperationResult<DomainModel> createRejectedResult(
			final EvaluatedCreationAttempt<DomainCreatePayload, DomainModel> evaluatedAttempt)
	{
		return CreationOperationResults.rejected(evaluatedAttempt.context(),
				List.copyOf(evaluatedAttempt.blockingViolations()),
				List.copyOf(evaluatedAttempt.toleratedViolations()));
	}

	private CreateOperationResult<DomainModel> createQuarantinedResult(
			final EvaluatedCreationAttempt<DomainCreatePayload, DomainModel> evaluatedAttempt)
	{
		return CreationOperationResults.quarantined(evaluatedAttempt.context(),
				List.copyOf(evaluatedAttempt.blockingViolations()), List.copyOf(evaluatedAttempt.toleratedViolations()),
				recordQuarantine(evaluatedAttempt));
	}

	@SuppressWarnings("unchecked")
	private Class<DomainCreatePayload> payloadTypeOf(final CreateOperationRequest<DomainCreatePayload> request)
	{
		return (Class<DomainCreatePayload>) request.payload().getClass();
	}

	private Optional<String> recordQuarantine(
			final EvaluatedCreationAttempt<DomainCreatePayload, DomainModel> evaluatedAttempt)
	{
		return evaluatedAttempt.quarantineReference().or(() -> quarantineRecorder.record(evaluatedAttempt));
	}

	protected AbstractCreateApplicationService(final CreationHandlerRegistry<DomainModel> handlerRegistry,
	                                           final CreationPolicyEvaluator policyEvaluator,
	                                           final CreateExecutor<DomainCreatePayload, DomainModel> createExecutor)
	{
		this(handlerRegistry, policyEvaluator, createExecutor, CreationQuarantineRecorder.noop());
	}

	protected AbstractCreateApplicationService(final CreationHandlerRegistry<DomainModel> handlerRegistry,
	                                           final CreationPolicyEvaluator policyEvaluator,
	                                           final CreateExecutor<DomainCreatePayload, DomainModel> createExecutor,
	                                           final CreationQuarantineRecorder quarantineRecorder)
	{
		this.handlerRegistry = Objects.requireNonNull(handlerRegistry, "handlerRegistry");
		this.policyEvaluator = Objects.requireNonNull(policyEvaluator, "policyEvaluator");
		this.createExecutor = Objects.requireNonNull(createExecutor, "createExecutor");
		this.quarantineRecorder = Objects.requireNonNull(quarantineRecorder, "quarantineRecorder");
	}
}