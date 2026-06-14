package de.gupta.clean.crud.template.useCases.operation.create.domain.policy;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;

@FunctionalInterface
public interface CreationPolicy
{
	CreationPolicyEvaluation evaluate(CreationOperationRequest<?> request, CreationPlan<?> plan);
}
