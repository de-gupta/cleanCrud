package de.gupta.clean.crud.template.useCases.crud.save.facade;

import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.APIToDomainCreateAdapter;
import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.DomainToAPIResponseAdapter;
import de.gupta.clean.crud.template.useCases.crud.save.application.service.SaveService;

import java.util.Collection;
import java.util.stream.Collectors;

public abstract class AbstractSaveServiceFacade<APIModelCreate, APIModelResponse,
		DomainModelCreate, DomainModelResponse, DomainID>
		implements SaveServiceFacade<APIModelCreate, APIModelResponse>
{
	private final SaveService<DomainModelCreate, DomainModelResponse, DomainID> service;
	private final APIToDomainCreateAdapter<APIModelCreate, DomainModelCreate> createMapper;
	private final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper;

	@Override
	public APIModelResponse save(final APIModelCreate model)
	{
		return responseMapper.mapToAPIModelResponse(service.save(createMapper.mapToDomainModelCreate(model)));
	}

	@Override
	public Collection<APIModelResponse> saveAll(final Collection<APIModelCreate> models)
	{
		return service.saveAll(models.stream()
									 .map(createMapper::mapToDomainModelCreate)
									 .collect(Collectors.toList()))
					  .stream()
					  .map(responseMapper::mapToAPIModelResponse)
					  .collect(Collectors.toList());
	}

	protected AbstractSaveServiceFacade(
			final SaveService<DomainModelCreate, DomainModelResponse, DomainID> service,
			final APIToDomainCreateAdapter<APIModelCreate, DomainModelCreate> createMapper,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper)
	{
		this.service = service;
		this.createMapper = createMapper;
		this.responseMapper = responseMapper;
	}
}