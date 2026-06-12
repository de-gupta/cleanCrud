package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.access;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;

import java.util.Optional;

@FunctionalInterface
public interface CreationAccessPolicy<DomainModel>
{
	static <DomainModel> CreationAccessPolicy<DomainModel> allowing()
	{
		return (_, _) ->
		{
		};
	}

	default Optional<String> accessViolationFor(
			final OperationSource source,
			final DomainModel afterModel)
	{
		try
		{
			validateAccess(source, afterModel);
			return Optional.empty();
		}
		catch (RuntimeException e)
		{
			return Optional.ofNullable(e.getMessage()).or(() -> Optional.of("Creation access rejected"));
		}
	}

	void validateAccess(final OperationSource source, final DomainModel afterModel);
}