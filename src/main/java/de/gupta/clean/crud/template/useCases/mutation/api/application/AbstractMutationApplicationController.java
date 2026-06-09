package de.gupta.clean.crud.template.useCases.mutation.api.application;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.mutation.application.service.MutationService;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationRequest;

public abstract class AbstractMutationApplicationController<DomainId, DomainModel>
		implements MutationApplicationController<DomainId, DomainModel>
{
	private final MutationService<DomainId, DomainModel> service;

	@Override
	public IdentifiedModel<DomainId, DomainModel> apply(final MutationRequest<DomainId, ?> request)
	{
		return service.mutate(request);
	}

	protected AbstractMutationApplicationController(final MutationService<DomainId, DomainModel> service)
	{
		this.service = service;
	}
}
