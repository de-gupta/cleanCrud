package de.gupta.clean.crud.template.useCases.incantation.domain.policy.invariant;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationSource;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant.InvariantViolation;

import java.util.List;

@FunctionalInterface
public interface IncantationInvariantPolicy<DomainModel>
{
	static <DomainModel> IncantationInvariantPolicy<DomainModel> allowing()
	{
		return (_, _) -> List.of();
	}

	List<InvariantViolation> invariantViolationsFor(
			final IncantationSource source,
			final DomainModel afterModel);
}
