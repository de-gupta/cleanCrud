package de.gupta.clean.crud.template.useCases.crud.fetch.api.application;

import de.gupta.clean.crud.template.useCases.crud.fetch.facade.FetchServiceFacade;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class AbstractFetchApplicationController<WebModelResponse, WebModelID>
		implements FetchApplicationController<WebModelResponse, WebModelID>
{
	private final FetchServiceFacade<WebModelResponse, WebModelID> service;

	@Override
	public Collection<WebModelResponse> findAll()
	{
		return service.findAll();
	}

	@Override
	public WebModelResponse findById(final WebModelID id)
	{
		return service.findById(id);
	}

	@Override
	public Map<WebModelID, WebModelResponse> findAllByIds(final Set<WebModelID> ids)
	{
		return service.findByIds(ids);
	}

	protected AbstractFetchApplicationController(
			final FetchServiceFacade<WebModelResponse, WebModelID> service)
	{
		this.service = service;
	}
}