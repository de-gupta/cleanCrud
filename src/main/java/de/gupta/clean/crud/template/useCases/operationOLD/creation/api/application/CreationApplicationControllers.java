package de.gupta.clean.crud.template.useCases.operationOLD.creation.api.application;

import de.gupta.clean.crud.template.useCases.operationOLD.creation.application.service.CreationService;

public final class CreationApplicationControllers
{
	public static <DomainId, DomainModel> CreationApplicationController<DomainId, DomainModel> controller(
			final CreationService<DomainId, DomainModel> service)
	{
		return new DefaultCreationApplicationController<>(service);
	}

	private CreationApplicationControllers()
	{
	}
}