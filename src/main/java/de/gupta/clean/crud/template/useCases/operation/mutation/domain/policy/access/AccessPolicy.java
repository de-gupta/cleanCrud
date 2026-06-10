package de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.access;

import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;

import java.util.Optional;

@FunctionalInterface
public interface AccessPolicy<DomainModel>
{
	static <DomainModel> AccessPolicy<DomainModel> allowing()
	{
		return (_, _, _) -> Optional.empty();
	}

	Optional<String> accessViolationFor(
			final OperationSource source,
			final DomainModel beforeModel,
			final DomainModel afterModel);
}
