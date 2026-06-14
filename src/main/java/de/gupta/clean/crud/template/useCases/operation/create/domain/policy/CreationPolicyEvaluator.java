package de.gupta.clean.crud.template.useCases.operation.create.domain.policy;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;

import java.util.Collection;

@FunctionalInterface
public interface CreationPolicyEvaluator
{
	static CreationPolicyEvaluator allowing()
	{
		return (_, _) -> CreationPolicyEvaluation.allow();
	}

	static CreationPolicyEvaluator of(final Collection<? extends CreationPolicy> policies)
	{
		return new DefaultCreationPolicyEvaluator(policies);
	}

	CreationPolicyEvaluation evaluate(CreationOperationRequest<?> request, CreationPlan<?> plan);
}
