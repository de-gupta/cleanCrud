package de.gupta.clean.crud.template.useCases.incantation.api.application;

import de.gupta.clean.crud.template.useCases.incantation.application.service.IncantationService;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationRequest;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationResult;

public abstract class AbstractIncantationApplicationController<DomainId, DomainModel>
		implements IncantationApplicationController<DomainId, DomainModel>
{
	private final IncantationService<DomainId, DomainModel> service;

	@Override
	public IncantationResult<DomainId, DomainModel> invokeWithResult(final IncantationRequest<?> request)
	{
		return service.incantWithResult(request);
	}

	protected AbstractIncantationApplicationController(final IncantationService<DomainId, DomainModel> service)
	{
		this.service = service;
	}
}
