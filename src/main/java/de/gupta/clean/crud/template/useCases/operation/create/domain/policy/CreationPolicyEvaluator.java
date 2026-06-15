package de.gupta.clean.crud.template.useCases.operation.create.domain.policy;

import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.PreparedCreationAttempt;

import java.util.Collection;

@FunctionalInterface
public interface CreationPolicyEvaluator
{
	static CreationPolicyEvaluator allowing()
	{
		return _ -> CreationPolicyEvaluation.allow();
	}

	static CreationPolicyEvaluator of(final Collection<? extends CreationPolicy> policies)
	{
		return new DefaultCreationPolicyEvaluator(policies);
	}

	CreationPolicyEvaluation evaluate(PreparedCreationAttempt<?, ?> preparedAttempt);
}