package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.creation;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;

import java.util.Optional;

@FunctionalInterface
public interface CreationPolicy<DomainModel>
{
	static <DomainModel> CreationPolicy<DomainModel> allowing()
	{
		return (_, _) -> Optional.empty();
	}

	default Optional<String> creationViolationFor(
			final OperationSource source,
			final DomainModel afterModel)
	{
		return validateCreation(source, afterModel);
	}

	Optional<String> validateCreation(final OperationSource source, final DomainModel afterModel);
}