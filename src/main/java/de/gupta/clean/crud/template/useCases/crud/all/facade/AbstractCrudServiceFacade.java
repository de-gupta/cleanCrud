package de.gupta.clean.crud.template.useCases.crud.all.facade;

import de.gupta.clean.crud.template.useCases.crud.all.application.service.CrudService;
import de.gupta.clean.crud.template.useCases.crud.all.facade.adapter.model.CrudAPIDomainModelAdapter;
import de.gupta.clean.crud.template.useCases.crud.common.adapter.id.APIDomainIDAdapter;
import de.gupta.clean.crud.template.useCases.crud.common.utility.PageUtility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public abstract class AbstractCrudServiceFacade<APIModelCreate, APIModelUpdate, APIModelResponse, APIModelID,
		DomainModelCreate, DomainModelUpdate, DomainModelResponse, DomainID>
		implements CrudServiceFacade<APIModelCreate, APIModelUpdate, APIModelResponse, APIModelID>
{
	private final CrudService<DomainModelCreate, DomainModelUpdate, DomainModelResponse, DomainID> service;
	private final CrudAPIDomainModelAdapter<APIModelCreate, APIModelUpdate, APIModelResponse,
			DomainID, DomainModelCreate, DomainModelUpdate, DomainModelResponse> modelAdapter;
	private final APIDomainIDAdapter<APIModelID, DomainID> idAdapter;

	@Override
	public Collection<APIModelResponse> findAll()
	{
		return service.findAll().stream()
					  .map(modelAdapter::mapToWebModelResponse)
					  .toList();
	}

	@Override
	public Page<APIModelResponse> findAll(final Pageable pageable)
	{
		return PageUtility.mapPage(service.findAll(pageable), modelAdapter::mapToWebModelResponse);
	}

	@Override
	public APIModelResponse findById(final APIModelID id)
	{
		return modelAdapter.mapToWebModelResponse(service.findById(idAdapter.mapToDomainID(id)));
	}

	@Override
	public APIModelResponse save(final APIModelCreate model)
	{
		return modelAdapter.mapToWebModelResponse(service.save(modelAdapter.mapToDomainModelCreate(model)));
	}

	@Override
	public Collection<APIModelResponse> saveAll(final Collection<APIModelCreate> models)
	{
		return service.saveAll(models.stream()
									 .map(modelAdapter::mapToDomainModelCreate)
									 .toList())
					  .stream()
					  .map(modelAdapter::mapToWebModelResponse)
					  .toList();
	}

	@Override
	public void putAtId(final APIModelID id, final APIModelCreate model)
	{
		service.putAtId(idAdapter.mapToDomainID(id), modelAdapter.mapToDomainModelCreate(model));
	}

	@Override
	public APIModelResponse updateById(final APIModelID id, final APIModelUpdate updatePatch)
	{
		return modelAdapter.mapToWebModelResponse(
				service.updateById(idAdapter.mapToDomainID(id), modelAdapter.mapToDomainModelUpdatePatch(updatePatch)));
	}

	@Override
	public void deleteById(final APIModelID id)
	{
		service.deleteById(idAdapter.mapToDomainID(id));
	}

	protected AbstractCrudServiceFacade(
			final CrudService<DomainModelCreate, DomainModelUpdate, DomainModelResponse, DomainID> service,
			final CrudAPIDomainModelAdapter<APIModelCreate, APIModelUpdate, APIModelResponse, DomainID,
					DomainModelCreate, DomainModelUpdate, DomainModelResponse> modelAdapter,
			final APIDomainIDAdapter<APIModelID, DomainID> idAdapter)
	{
		this.service = service;
		this.modelAdapter = modelAdapter;
		this.idAdapter = idAdapter;
	}
}