package de.gupta.clean.crud.template.useCases.mutation.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationResult;

@FunctionalInterface
public interface MutationService<DomainId, DomainModel>
{
	default IdentifiedModel<DomainId, DomainModel> mutate(final MutationRequest<DomainId, ?> request)
	{
		return mutateWithResult(request).updatedOrThrow();
	}

	MutationResult<DomainId, DomainModel> mutateWithResult(final MutationRequest<DomainId, ?> request);
}
