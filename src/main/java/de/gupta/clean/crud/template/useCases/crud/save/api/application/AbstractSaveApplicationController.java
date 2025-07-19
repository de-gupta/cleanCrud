package de.gupta.clean.crud.template.useCases.crud.save.api.application;

import de.gupta.clean.crud.template.useCases.crud.save.facade.SaveServiceFacade;

import java.util.Collection;

public abstract class AbstractSaveApplicationController<WebModelCreate, WebModelResponse>
		implements SaveApplicationController<WebModelCreate, WebModelResponse>
{
	private final SaveServiceFacade<WebModelCreate, WebModelResponse> service;

	@Override
	public WebModelResponse save(final WebModelCreate model)
	{
		return service.save(model);
	}

	@Override
	public Collection<WebModelResponse> saveAll(final Collection<WebModelCreate> models)
	{
		return service.saveAll(models);
	}

	protected AbstractSaveApplicationController(final SaveServiceFacade<WebModelCreate, WebModelResponse> service)
	{
		this.service = service;
	}
}