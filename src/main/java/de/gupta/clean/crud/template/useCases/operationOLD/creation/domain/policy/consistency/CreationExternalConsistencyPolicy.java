package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.consistency;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;

import java.util.Optional;

@FunctionalInterface
public interface CreationExternalConsistencyPolicy<DomainModel>
{
	static <DomainModel> CreationExternalConsistencyPolicy<DomainModel> allowing()
	{
		return (_, _) -> Optional.empty();
	}

	Optional<String> consistencyViolationFor(final OperationSource source, final DomainModel afterModel);
}