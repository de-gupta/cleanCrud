package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.evaluation;

import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.quarantine.QuarantinedCreationException;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;

@FunctionalInterface
public interface SourceAwareCreationPolicy<DomainModel>
{
	static <DomainModel> SourceAwareCreationPolicy<DomainModel> allowing()
	{
		return (_, _) -> CreationPolicyDecision.allow();
	}

	CreationPolicyDecision evaluate(
			final OperationSource source,
			final DomainModel afterModel);

	default void validate(
			final OperationSource source,
			final DomainModel afterModel)
	{
		var decision = evaluate(source, afterModel);
		if (decision.quarantined())
		{
			throw QuarantinedCreationException.withRequest(decision.quarantineRequest().orElseThrow());
		}
	}
}