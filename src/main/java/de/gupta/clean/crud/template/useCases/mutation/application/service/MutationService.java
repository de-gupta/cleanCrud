package de.gupta.clean.crud.template.useCases.mutation.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationRequest;

public interface MutationService<DomainId, DomainModel>
{
	IdentifiedModel<DomainId, DomainModel> mutate(final MutationRequest<DomainId, ?> request);
}
