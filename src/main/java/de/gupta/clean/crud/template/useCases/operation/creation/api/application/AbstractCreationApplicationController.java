package de.gupta.clean.crud.template.useCases.operation.creation.api.application;

import de.gupta.clean.crud.template.useCases.operation.creation.application.service.CreationService;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationResult;

public abstract class AbstractCreationApplicationController<DomainId, DomainModel>
		implements CreationApplicationController<DomainId, DomainModel>
{
	private final CreationService<DomainId, DomainModel> service;

	@Override
	public CreationResult<DomainId, DomainModel> createWithResult(final CreationRequest<?> request)
	{
		return service.createWithResult(request);
	}

	protected AbstractCreationApplicationController(final CreationService<DomainId, DomainModel> service)
	{
		this.service = service;
	}
}
