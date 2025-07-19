package de.gupta.clean.crud.template.useCases.crud.update.api.application;

import de.gupta.clean.crud.template.useCases.crud.update.facade.UpdateServiceFacade;

public abstract class AbstractUpdateApplicationController<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID>
		implements UpdateApplicationController<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID>
{
	private final UpdateServiceFacade<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID> service;

	@Override
	public void putAtId(final WebModelID id, final WebModelCreate model)
	{
		service.putAtId(id, model);
	}

	@Override
	public WebModelResponse updateById(final WebModelID id, final WebModelUpdatePatch model)
	{
		return service.updateById(id, model);
	}

	protected AbstractUpdateApplicationController(
			final UpdateServiceFacade<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID> service)
	{
		this.service = service;
	}
}