package de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.access;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;

@FunctionalInterface
public interface MutationAccessPolicy<DomainModel>
{
	void validateAccess(final OperationSource source, final DomainModel beforeModel, final DomainModel afterModel);
}