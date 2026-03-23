package de.gupta.clean.crud.template.useCases.crud.delete.api.application;

import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import de.gupta.clean.crud.template.useCases.crud.delete.facade.DeleteServiceFacade;

import java.util.Collection;

public abstract class AbstractDeleteApplicationController<WebModelID> implements DeleteApplicationController<WebModelID>
{
	private final DeleteServiceFacade<WebModelID> service;

	@Override
	public void deleteById(final WebModelID id)
	{
		service.deleteById(id);
	}

	@Override
	public void deleteAllById(final Collection<WebModelID> ids, final BulkOperationMode mode)
	{
		service.deleteAllById(ids, mode);
	}

	protected AbstractDeleteApplicationController(final DeleteServiceFacade<WebModelID> service)
	{
		this.service = service;
	}
}
