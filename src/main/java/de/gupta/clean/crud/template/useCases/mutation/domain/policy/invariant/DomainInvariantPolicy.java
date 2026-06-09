package de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;

import java.util.Collection;
import java.util.List;

@FunctionalInterface
public interface DomainInvariantPolicy<DomainModel>
{
	static <DomainModel> DomainInvariantPolicy<DomainModel> allowing()
	{
		return (_, _, _) -> List.of();
	}

	Collection<InvariantViolation> invariantViolationsFor(
			final MutationSource source,
			final DomainModel beforeModel,
			final DomainModel afterModel);
}
