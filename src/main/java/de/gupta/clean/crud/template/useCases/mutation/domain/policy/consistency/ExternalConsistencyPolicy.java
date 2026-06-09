package de.gupta.clean.crud.template.useCases.mutation.domain.policy.consistency;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;

import java.util.Optional;

@FunctionalInterface
public interface ExternalConsistencyPolicy<DomainModel>
{
	static <DomainModel> ExternalConsistencyPolicy<DomainModel> allowing()
	{
		return (_, _, _) -> Optional.empty();
	}

	Optional<String> consistencyViolationFor(
			final MutationSource source,
			final DomainModel beforeModel,
			final DomainModel afterModel);
}
