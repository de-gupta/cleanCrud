package de.gupta.clean.crud.template.useCases.query.suppliedProperty.facade;

import de.gupta.clean.crud.template.useCases.crud.common.adapter.model.DomainToAPIResponseAdapter;
import de.gupta.clean.crud.template.useCases.query.property.application.service.PropertyQueryService;
import de.gupta.commons.utility.comparison.ComparisonType;

import java.util.Collection;
import java.util.function.Supplier;

public abstract class AbstractPropertySuppliedQueryServiceFacade<Property, DomainID, DomainModelResponse, APIModelResponse>
		implements PropertySuppliedQueryServiceFacade<APIModelResponse>
{
	private final PropertyQueryService<Property, DomainID, DomainModelResponse> service;
	private final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper;
	private final Supplier<Property> propertyValueSupplier;

	@Override
	public Collection<APIModelResponse> queryBySuppliedProperty(final String propertyName,
																final ComparisonType comparisonType)
	{
		return service.queryBy(propertyName, propertyValueSupplier.get(), comparisonType).stream()
					  .map(responseMapper::mapToAPIModelResponse).toList();
	}

	protected AbstractPropertySuppliedQueryServiceFacade(
			final PropertyQueryService<Property, DomainID, DomainModelResponse> service,
			final DomainToAPIResponseAdapter<APIModelResponse, DomainID, DomainModelResponse> responseMapper,
			final Supplier<Property> propertyValueSupplier)
	{
		this.service = service;
		this.responseMapper = responseMapper;
		this.propertyValueSupplier = propertyValueSupplier;
	}
}