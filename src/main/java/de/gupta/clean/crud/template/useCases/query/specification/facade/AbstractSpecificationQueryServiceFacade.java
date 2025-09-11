package de.gupta.clean.crud.template.useCases.query.specification.facade;

import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.DomainToAPIResponseAdapter;
import de.gupta.clean.crud.template.useCases.query.specification.application.service.SpecificationQueryService;
import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;

import java.util.Collection;

public abstract class AbstractSpecificationQueryServiceFacade<APIModelResponse, DomainID, DomainModelResponse>
		implements SpecificationQueryServiceFacade<APIModelResponse>
{
	private final SpecificationQueryService<DomainID, DomainModelResponse> service;
	private final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper;

	@Override
	public Collection<APIModelResponse> queryBy(final FilterSpecification filterSpecification)
	{
		return service.queryBy(filterSpecification).stream().map(responseMapper::mapToAPIModelResponse).toList();
	}

	protected AbstractSpecificationQueryServiceFacade(
			final SpecificationQueryService<DomainID, DomainModelResponse> service,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper)
	{
		this.service = service;
		this.responseMapper = responseMapper;
	}
}