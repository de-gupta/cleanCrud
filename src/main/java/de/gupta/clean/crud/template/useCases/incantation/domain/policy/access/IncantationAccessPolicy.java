package de.gupta.clean.crud.template.useCases.incantation.domain.policy.access;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationSource;

import java.util.Optional;

@FunctionalInterface
public interface IncantationAccessPolicy<DomainModel>
{
	static <DomainModel> IncantationAccessPolicy<DomainModel> allowing()
	{
		return (_, _) ->
		{
		};
	}

	default Optional<String> accessViolationFor(
			final IncantationSource source,
			final DomainModel afterModel)
	{
		try
		{
			validateAccess(source, afterModel);
			return Optional.empty();
		}
		catch (RuntimeException e)
		{
			return Optional.ofNullable(e.getMessage()).or(() -> Optional.of("Incantation access rejected"));
		}
	}

	void validateAccess(final IncantationSource source, final DomainModel afterModel);
}
