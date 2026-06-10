package de.gupta.clean.crud.template.useCases.operation.creation.api.application;

import de.gupta.clean.crud.template.useCases.operation.creation.application.service.CreationService;

public final class DefaultCreationApplicationController<DomainId, DomainModel>
		extends AbstractCreationApplicationController<DomainId, DomainModel>
{
	public DefaultCreationApplicationController(final CreationService<DomainId, DomainModel> service)
	{
		super(service);
	}
}
