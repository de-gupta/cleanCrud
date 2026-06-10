package de.gupta.clean.crud.template.useCases.incantation.domain.policy.consistency;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationSource;

import java.util.Optional;

@FunctionalInterface
public interface IncantationExternalConsistencyPolicy<DomainModel>
{
	static <DomainModel> IncantationExternalConsistencyPolicy<DomainModel> allowing()
	{
		return (_, _) -> Optional.empty();
	}

	Optional<String> consistencyViolationFor(final IncantationSource source, final DomainModel afterModel);
}
