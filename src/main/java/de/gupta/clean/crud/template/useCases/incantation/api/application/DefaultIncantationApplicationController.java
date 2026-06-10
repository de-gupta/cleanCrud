package de.gupta.clean.crud.template.useCases.incantation.api.application;

import de.gupta.clean.crud.template.useCases.incantation.application.service.IncantationService;

public final class DefaultIncantationApplicationController<DomainId, DomainModel>
		extends AbstractIncantationApplicationController<DomainId, DomainModel>
{
	public DefaultIncantationApplicationController(final IncantationService<DomainId, DomainModel> service)
	{
		super(service);
	}
}
