package de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.invariant;

import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;

@FunctionalInterface
public interface MutationInvariantPolicy<DomainModel>
{
	void validateInvariant(
			final OperationSource source,
			final DomainModel beforeModel,
			final DomainModel afterModel);
}
