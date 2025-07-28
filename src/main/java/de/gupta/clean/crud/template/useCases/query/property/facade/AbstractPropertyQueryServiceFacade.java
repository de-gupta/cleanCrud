package de.gupta.clean.crud.template.useCases.query.property.facade;

import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.DomainToAPIResponseAdapter;
import de.gupta.clean.crud.template.useCases.query.property.application.service.PropertyQueryService;
import de.gupta.commons.utility.comparison.ComparisonType;

import java.util.Collection;

public abstract class AbstractPropertyQueryServiceFacade<Property, DomainID, DomainModelResponse, APIModelResponse>
		implements PropertyQueryServiceFacade<Property, APIModelResponse>
{
	private final PropertyQueryService<Property, DomainID, DomainModelResponse> service;
	private final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper;

	@Override
	public Collection<APIModelResponse> findAll(final String propertyName, final Property propertyValue,
												final ComparisonType comparisonType)
	{
		return service.findAll(propertyName, propertyValue, comparisonType).stream()
					  .map(responseMapper::mapToAPIModelResponse).toList();
	}

	protected AbstractPropertyQueryServiceFacade(
			final PropertyQueryService<Property, DomainID, DomainModelResponse> service,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper)
	{
		this.service = service;
		this.responseMapper = responseMapper;
	}
}