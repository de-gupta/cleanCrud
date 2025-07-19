package de.gupta.clean.crud.template.useCases.crud.delete.facade;

import de.gupta.clean.crud.template.useCases.crud.common.adapter.id.APIDomainIDAdapter;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.DeleteService;

public abstract class AbstractDeleteServiceFacade<APIModelID, DomainID>
		implements DeleteServiceFacade<APIModelID>
{
	private final DeleteService<DomainID> service;
	private final APIDomainIDAdapter<APIModelID, DomainID> idAdapter;

	@Override
	public void deleteById(final APIModelID id)
	{
		service.deleteById(idAdapter.mapToDomainID(id));
	}

	protected AbstractDeleteServiceFacade(
			final DeleteService<DomainID> service,
			final APIDomainIDAdapter<APIModelID, DomainID> idAdapter)
	{
		this.service = service;
		this.idAdapter = idAdapter;
	}
}