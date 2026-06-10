package de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.transition;

import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;

import java.util.Optional;

@FunctionalInterface
public interface MutationTransitionPolicy<DomainModel>
{
	static <DomainModel> MutationTransitionPolicy<DomainModel> allowing()
	{
		return (_, _, _) -> Optional.empty();
	}

	Optional<String> transitionViolationFor(
			final OperationSource source,
			final DomainModel beforeModel,
			final DomainModel afterModel);
}
