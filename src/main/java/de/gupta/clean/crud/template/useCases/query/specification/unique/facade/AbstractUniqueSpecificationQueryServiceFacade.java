package de.gupta.clean.crud.template.useCases.query.specification.unique.facade;

import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.DomainToAPIResponseAdapter;
import de.gupta.clean.crud.template.useCases.query.specification.collection.application.service.SpecificationQueryService;
import de.gupta.clean.crud.template.useCases.query.specification.domain.model.FilterSpecification;

import java.util.Optional;

public abstract class AbstractUniqueSpecificationQueryServiceFacade<DomainID, APIModelResponse, DomainModelResponse>
		implements UniqueSpecificationQueryServiceFacade<APIModelResponse>
{
	private final SpecificationQueryService<DomainID, DomainModelResponse> service;
	private final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper;

	@Override
	public Optional<APIModelResponse> queryUniqueBy(final FilterSpecification filterSpecification)
	{
		return service.queryBy(filterSpecification)
					  .stream()
					  .map(responseMapper::mapToAPIModelResponse)
					  .findFirst();
	}

	protected AbstractUniqueSpecificationQueryServiceFacade(
			final SpecificationQueryService<DomainID, DomainModelResponse> service,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper)
	{
		this.service = service;
		this.responseMapper = responseMapper;
	}
}