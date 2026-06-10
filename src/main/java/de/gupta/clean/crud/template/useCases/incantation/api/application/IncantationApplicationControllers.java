package de.gupta.clean.crud.template.useCases.incantation.api.application;

import de.gupta.clean.crud.template.useCases.incantation.application.service.IncantationService;

public final class IncantationApplicationControllers
{
	public static <DomainId, DomainModel> IncantationApplicationController<DomainId, DomainModel> controller(
			final IncantationService<DomainId, DomainModel> service)
	{
		return new DefaultIncantationApplicationController<>(service);
	}

	private IncantationApplicationControllers()
	{
	}
}
