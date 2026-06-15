package de.gupta.clean.crud.template.useCases.operation.create.domain.policy;

import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.PreparedCreationAttempt;

@FunctionalInterface
public interface CreationPolicy<DomainModel>
{
	CreationPolicyEvaluation evaluate(PreparedCreationAttempt<?, DomainModel> preparedAttempt);
}