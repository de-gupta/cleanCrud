package de.gupta.clean.crud.template.useCases.operation.create.application.service;

import de.gupta.clean.crud.template.useCases.operation.create.domain.execution.CreationExecutor;
import de.gupta.clean.crud.template.useCases.operation.create.domain.handler.CreationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationContext;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.policy.CreationPolicyEvaluation;
import de.gupta.clean.crud.template.useCases.operation.create.domain.policy.CreationPolicyEvaluator;
import de.gupta.clean.crud.template.useCases.operation.create.domain.quarantine.CreationQuarantineRecordRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.quarantine.CreationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreateOperationResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationResults;

import java.util.Objects;
import java.util.Optional;

public abstract class AbstractCreateApplicationService<Payload extends CreateOperationPayload, DomainCreateModel, DomainModel>
		implements CreateApplicationService<Payload, DomainModel>
{
	private final CreationHandlerRegistry<DomainCreateModel> handlerRegistry;
	private final CreationPolicyEvaluator policyEvaluator;
	private final CreationExecutor<DomainCreateModel, DomainModel> creationExecutor;
	private final CreationQuarantineRecorder quarantineRecorder;

	@Override
	public CreateOperationResult<DomainModel> create(final CreationOperationRequest<Payload> request)
	{
		var context = CreationOperationContext.from(request);
		var handler = handlerRegistry.resolveHandlerFor(request.payload().getClass());
		var plan = handler.createPlan(request);
		var evaluation = policyEvaluator.evaluate(request, plan);

		return switch (evaluation.decision())
		{
			case ALLOW -> CreationOperationResults.created(
					context,
					creationExecutor.create(plan),
					evaluation.toleratedViolations().stream().toList());
			case REJECT -> CreationOperationResults.rejected(
					context,
					evaluation.blockingViolations().stream().toList(),
					evaluation.toleratedViolations().stream().toList());
			case QUARANTINE -> CreationOperationResults.quarantined(
					context,
					evaluation.blockingViolations().stream().toList(),
					evaluation.toleratedViolations().stream().toList(),
					recordQuarantine(context, request, plan.aggregateKey(), evaluation));
		};
	}


	private Optional<String> recordQuarantine(
			final CreationOperationContext context,
			final CreationOperationRequest<?> request,
			final String aggregateKey,
			final CreationPolicyEvaluation evaluation)
	{
		return evaluation.quarantineReference().or(() -> quarantineRecorder.record(new CreationQuarantineRecordRequest(
				context,
				aggregateKey,
				request,
				evaluation.blockingViolations(),
				evaluation.toleratedViolations())));
	}

	protected AbstractCreateApplicationService(
			final CreationHandlerRegistry<DomainCreateModel> handlerRegistry,
			final CreationPolicyEvaluator policyEvaluator,
			final CreationExecutor<DomainCreateModel, DomainModel> creationExecutor)
	{
		this(handlerRegistry, policyEvaluator, creationExecutor, CreationQuarantineRecorder.noop());
	}

	protected AbstractCreateApplicationService(
			final CreationHandlerRegistry<DomainCreateModel> handlerRegistry,
			final CreationPolicyEvaluator policyEvaluator,
			final CreationExecutor<DomainCreateModel, DomainModel> creationExecutor,
			final CreationQuarantineRecorder quarantineRecorder)
	{
		this.handlerRegistry = Objects.requireNonNull(handlerRegistry, "handlerRegistry");
		this.policyEvaluator = Objects.requireNonNull(policyEvaluator, "policyEvaluator");
		this.creationExecutor = Objects.requireNonNull(creationExecutor, "creationExecutor");
		this.quarantineRecorder = Objects.requireNonNull(quarantineRecorder, "quarantineRecorder");
	}
}