package de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.consistency;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;

import java.util.Optional;

@FunctionalInterface
public interface ExternalConsistencyPolicy<DomainModel>
{
	static <DomainModel> ExternalConsistencyPolicy<DomainModel> allowing()
	{
		return (_, _, _) -> Optional.empty();
	}

	Optional<String> consistencyViolationFor(
			final OperationSource source,
			final DomainModel beforeModel,
			final DomainModel afterModel);
}