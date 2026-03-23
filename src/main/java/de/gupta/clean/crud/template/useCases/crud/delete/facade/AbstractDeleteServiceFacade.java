package de.gupta.clean.crud.template.useCases.crud.delete.facade;

import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import de.gupta.clean.crud.template.useCases.crud.common.adapter.id.APIDomainIDAdapter;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.DeleteService;

import java.util.Collection;

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

	@Override
	public void deleteAllById(final Collection<APIModelID> ids, final BulkOperationMode mode)
	{
		service.deleteAllById(ids.stream().map(idAdapter::mapToDomainID).toList(), mode);
	}

	protected AbstractDeleteServiceFacade(
			final DeleteService<DomainID> service,
			final APIDomainIDAdapter<APIModelID, DomainID> idAdapter)
	{
		this.service = service;
		this.idAdapter = idAdapter;
	}
}
