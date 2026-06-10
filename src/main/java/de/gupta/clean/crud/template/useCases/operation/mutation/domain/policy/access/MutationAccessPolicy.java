package de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.access;

import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;

@FunctionalInterface
public interface MutationAccessPolicy<DomainModel>
{
	void validateAccess(final OperationSource source, final DomainModel beforeModel, final DomainModel afterModel);
}