package de.gupta.clean.crud.template.useCases.operation.create.domain.attempt;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationContext;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;
import de.gupta.clean.crud.template.useCases.operation.create.domain.policy.CreationDecision;
import de.gupta.clean.crud.template.useCases.operation.create.domain.policy.CreationPolicyEvaluation;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationViolation;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public record EvaluatedCreationAttempt<Payload extends CreateOperationPayload, DomainCreateModel>(
		PreparedCreationAttempt<Payload, DomainCreateModel> preparedAttempt,
		CreationPolicyEvaluation evaluation)
{
	public static <Payload extends CreateOperationPayload, DomainCreateModel> EvaluatedCreationAttempt<Payload, DomainCreateModel> of(
			final PreparedCreationAttempt<Payload, DomainCreateModel> preparedAttempt,
			final CreationPolicyEvaluation evaluation)
	{
		return new EvaluatedCreationAttempt<>(preparedAttempt, evaluation);
	}

	public EvaluatedCreationAttempt
	{
		Objects.requireNonNull(preparedAttempt, "preparedAttempt");
		Objects.requireNonNull(evaluation, "evaluation");
	}

	public CreationOperationRequest<Payload> request()
	{
		return preparedAttempt.request();
	}

	public CreationOperationContext context()
	{
		return preparedAttempt.context();
	}

	public CreationPlan<DomainCreateModel> plan()
	{
		return preparedAttempt.plan();
	}

	public String aggregateKey()
	{
		return preparedAttempt.aggregateKey();
	}

	public CreationDecision decision()
	{
		return evaluation.decision();
	}

	public Collection<CreationOperationViolation> blockingViolations()
	{
		return evaluation.blockingViolations();
	}

	public Collection<CreationOperationViolation> toleratedViolations()
	{
		return evaluation.toleratedViolations();
	}

	public Optional<String> quarantineReference()
	{
		return evaluation.quarantineReference();
	}
}