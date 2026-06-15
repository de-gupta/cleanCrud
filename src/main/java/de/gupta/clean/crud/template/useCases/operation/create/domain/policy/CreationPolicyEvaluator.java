package de.gupta.clean.crud.template.useCases.operation.create.domain.policy;

import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.PreparedCreationAttempt;

import java.util.Collection;

@FunctionalInterface
public interface CreationPolicyEvaluator<DomainModel>
{
	static <DomainModel> CreationPolicyEvaluator<DomainModel> allowing()
	{
		return _ -> CreationPolicyEvaluation.allow();
	}

	static <DomainModel> CreationPolicyEvaluator<DomainModel> of(
			final Collection<? extends CreationPolicy<DomainModel>> policies)
	{
		return DefaultCreationPolicyEvaluator.with(policies);
	}

	CreationPolicyEvaluation evaluate(PreparedCreationAttempt<?, DomainModel> preparedAttempt);
}