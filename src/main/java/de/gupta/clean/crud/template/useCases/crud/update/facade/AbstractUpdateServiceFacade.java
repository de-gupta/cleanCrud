package de.gupta.clean.crud.template.useCases.crud.update.facade;

import de.gupta.clean.crud.template.useCases.crud.common.adapter.id.APIDomainIDAdapter;
import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.APIToDomainCreateAdapter;
import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.APIToDomainUpdateAdapter;
import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.DomainToAPIResponseAdapter;
import de.gupta.clean.crud.template.useCases.crud.update.application.service.UpdateService;

public abstract class AbstractUpdateServiceFacade<APIModelCreate, APIModelUpdatePatch, APIModelResponse, APIModelID,
		DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse, DomainID>
		implements UpdateServiceFacade<APIModelCreate, APIModelUpdatePatch, APIModelResponse, APIModelID>
{
	private final UpdateService<DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse, DomainID> service;
	private final APIToDomainCreateAdapter<APIModelCreate, DomainModelCreate> createAdapter;
	private final APIToDomainUpdateAdapter<APIModelUpdatePatch, DomainModelUpdatePatch> updateAdapter;
	private final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseAdapter;
	private final APIDomainIDAdapter<APIModelID, DomainID> idAdapter;

	@Override
	public void putAtId(final APIModelID id, final APIModelCreate model)
	{
		service.putAtId(idAdapter.mapToDomainID(id), createAdapter.mapToDomainModelCreate(model));
	}

	@Override
	public APIModelResponse updateById(final APIModelID id, final APIModelUpdatePatch updatePatch)
	{
		return responseAdapter.mapToAPIModelResponse(
				service.updateById(idAdapter.mapToDomainID(id),
						updateAdapter.mapToDomainModelUpdatePatch(updatePatch)));
	}

	protected AbstractUpdateServiceFacade(
			final UpdateService<DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse, DomainID> service,
			final APIToDomainCreateAdapter<APIModelCreate, DomainModelCreate> createAdapter,
			final APIToDomainUpdateAdapter<APIModelUpdatePatch, DomainModelUpdatePatch> updateAdapter,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseAdapter,
			final APIDomainIDAdapter<APIModelID, DomainID> idAdapter)
	{
		this.service = service;
		this.createAdapter = createAdapter;
		this.updateAdapter = updateAdapter;
		this.responseAdapter = responseAdapter;
		this.idAdapter = idAdapter;
	}
}