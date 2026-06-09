package de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;

@FunctionalInterface
public interface MutationInvariantPolicy<DomainModel>
{
	void validateInvariant(
			final MutationSource source,
			final DomainModel beforeModel,
			final DomainModel afterModel);
}
