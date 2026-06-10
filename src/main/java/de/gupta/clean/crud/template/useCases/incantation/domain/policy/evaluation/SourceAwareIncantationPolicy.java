package de.gupta.clean.crud.template.useCases.incantation.domain.policy.evaluation;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationSource;

@FunctionalInterface
public interface SourceAwareIncantationPolicy<DomainModel>
{
	static <DomainModel> SourceAwareIncantationPolicy<DomainModel> allowing()
	{
		return (_, _) -> IncantationPolicyDecision.allow();
	}

	IncantationPolicyDecision evaluate(
			final IncantationSource source,
			final DomainModel afterModel);
}
