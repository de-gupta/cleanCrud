package de.gupta.clean.crud.template.useCases.incantation.domain.policy.evaluation;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationSource;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.quarantine.QuarantinedIncantationException;

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

	default void validate(
			final IncantationSource source,
			final DomainModel afterModel)
	{
		var decision = evaluate(source, afterModel);
		if (decision.quarantined())
		{
			throw QuarantinedIncantationException.withRequest(decision.quarantineRequest().orElseThrow());
		}
	}
}
