package de.gupta.clean.crud.template.useCases.operationOLD.mutation.application.service;

import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.model.MutationResult;

@FunctionalInterface
public interface MutationService<DomainId, DomainModel>
{
	default MutationResult<DomainId, DomainModel> mutate(final MutationRequest<DomainId, ?> request)
	{
		return mutateWithResult(request);
	}

	MutationResult<DomainId, DomainModel> mutateWithResult(final MutationRequest<DomainId, ?> request);
}