package de.gupta.clean.crud.template.useCases.mutation.domain.policy.access;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;

import java.util.Optional;

@FunctionalInterface
public interface AccessPolicy<DomainModel>
{
	static <DomainModel> AccessPolicy<DomainModel> allowing()
	{
		return (_, _, _) -> Optional.empty();
	}

	Optional<String> accessViolationFor(
			final MutationSource source,
			final DomainModel beforeModel,
			final DomainModel afterModel);
}
