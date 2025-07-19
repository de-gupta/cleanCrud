package de.gupta.clean.crud.template.useCases.crud.delete.api.application;

import de.gupta.clean.crud.template.useCases.crud.delete.facade.DeleteServiceFacade;

public abstract class AbstractDeleteApplicationController<WebModelID> implements DeleteApplicationController<WebModelID>
{
	private final DeleteServiceFacade<WebModelID> service;

	@Override
	public void deleteById(final WebModelID id)
	{
		service.deleteById(id);
	}

	protected AbstractDeleteApplicationController(final DeleteServiceFacade<WebModelID> service)
	{
		this.service = service;
	}
}