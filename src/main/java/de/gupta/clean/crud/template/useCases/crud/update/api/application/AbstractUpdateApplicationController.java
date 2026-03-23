package de.gupta.clean.crud.template.useCases.crud.update.api.application;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import de.gupta.clean.crud.template.useCases.crud.update.facade.UpdateServiceFacade;

import java.util.Collection;

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

	@Override
	public Collection<WebModelResponse> updateAllById(
			final Collection<IdentifiedModel<WebModelID, WebModelUpdatePatch>> models,
			final BulkOperationMode mode)
	{
		return service.updateAllById(models, mode);
	}

	protected AbstractUpdateApplicationController(
			final UpdateServiceFacade<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID> service)
	{
		this.service = service;
	}
}
