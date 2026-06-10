package de.gupta.clean.crud.template.useCases.incantation.domain.policy.creation;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationSource;

import java.util.Optional;

@FunctionalInterface
public interface IncantationCreationPolicy<DomainModel>
{
	static <DomainModel> IncantationCreationPolicy<DomainModel> allowing()
	{
		return (_, _) -> Optional.empty();
	}

	default Optional<String> creationViolationFor(
			final IncantationSource source,
			final DomainModel afterModel)
	{
		return validateCreation(source, afterModel);
	}

	Optional<String> validateCreation(final IncantationSource source, final DomainModel afterModel);
}
