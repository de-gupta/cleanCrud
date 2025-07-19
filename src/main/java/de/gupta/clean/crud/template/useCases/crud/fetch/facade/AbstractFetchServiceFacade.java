package de.gupta.clean.crud.template.useCases.crud.fetch.facade;

import de.gupta.clean.crud.template.useCases.crud.common.adapter.id.APIDomainIDAdapter;
import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.DomainToAPIResponseAdapter;
import de.gupta.clean.crud.template.useCases.crud.common.utility.PageUtility;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.FetchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractFetchServiceFacade<APIModelResponse, APIModelID,
		DomainModelResponse, DomainID>
		implements FetchServiceFacade<APIModelResponse, APIModelID>
{
	private final FetchService<DomainModelResponse, DomainID> service;
	private final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper;
	private final APIDomainIDAdapter<APIModelID, DomainID> idAdapter;

	@Override
	public Collection<APIModelResponse> findAll()
	{
		return service.findAll().stream()
					  .map(responseMapper::mapToAPIModelResponse)
					  .toList();
	}

	@Override
	public Page<APIModelResponse> findAll(final Pageable pageable)
	{
		return PageUtility.mapPage(service.findAll(pageable), responseMapper::mapToAPIModelResponse);
	}

	@Override
	public APIModelResponse findById(final APIModelID id)
	{
		return responseMapper.mapToAPIModelResponse(
				service.findById(idAdapter.mapToDomainID(id)));
	}

	@Override
	public Map<APIModelID, APIModelResponse> findByIds(final Set<APIModelID> IDs)
	{
		final var domainIDs = IDs.stream().map(idAdapter::mapToDomainID).collect(Collectors.toSet());

		return service.findByIds(domainIDs)
					  .stream()
					  .collect(Collectors.toMap(im -> idAdapter.mapToAPIModelID(im.id()),
							  responseMapper::mapToAPIModelResponse));
	}

	protected AbstractFetchServiceFacade(
			final FetchService<DomainModelResponse, DomainID> service,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper,
			final APIDomainIDAdapter<APIModelID, DomainID> idAdapter)
	{
		this.service = service;
		this.responseMapper = responseMapper;
		this.idAdapter = idAdapter;
	}
}