package de.gupta.clean.crud.template.useCases.operationOLD.creation.api.application;

import de.gupta.clean.crud.template.useCases.operationOLD.creation.application.service.CreationService;

public final class DefaultCreationApplicationController<DomainId, DomainModel>
		extends AbstractCreationApplicationController<DomainId, DomainModel>
{
	public DefaultCreationApplicationController(final CreationService<DomainId, DomainModel> service)
	{
		super(service);
	}
}