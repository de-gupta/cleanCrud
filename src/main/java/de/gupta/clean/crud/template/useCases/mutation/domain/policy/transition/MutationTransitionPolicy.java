package de.gupta.clean.crud.template.useCases.mutation.domain.policy.transition;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;

import java.util.Optional;

@FunctionalInterface
public interface MutationTransitionPolicy<DomainModel>
{
	static <DomainModel> MutationTransitionPolicy<DomainModel> allowing()
	{
		return (_, _, _) -> Optional.empty();
	}

	Optional<String> transitionViolationFor(
			final MutationSource source,
			final DomainModel beforeModel,
			final DomainModel afterModel);
}
