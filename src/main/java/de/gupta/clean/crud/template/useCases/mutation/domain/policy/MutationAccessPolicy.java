package de.gupta.clean.crud.template.useCases.mutation.domain.policy;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;

@FunctionalInterface
public interface MutationAccessPolicy<DomainModel>
{
	void validateAccess(
			final MutationSource source,
			final DomainModel beforeModel,
			final DomainModel afterModel);
}
