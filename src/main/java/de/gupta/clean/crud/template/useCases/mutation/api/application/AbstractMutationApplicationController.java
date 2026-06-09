package de.gupta.clean.crud.template.useCases.mutation.api.application;

import de.gupta.clean.crud.template.useCases.mutation.application.service.MutationService;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationResult;

public abstract class AbstractMutationApplicationController<DomainId, DomainModel>
		implements MutationApplicationController<DomainId, DomainModel>
{
	private final MutationService<DomainId, DomainModel> service;

	@Override
	public MutationResult<DomainId, DomainModel> applyWithResult(final MutationRequest<DomainId, ?> request)
	{
		return service.mutateWithResult(request);
	}

	protected AbstractMutationApplicationController(final MutationService<DomainId, DomainModel> service)
	{
		this.service = service;
	}
}
