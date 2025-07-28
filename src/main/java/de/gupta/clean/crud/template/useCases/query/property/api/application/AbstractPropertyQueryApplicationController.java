package de.gupta.clean.crud.template.useCases.query.property.api.application;

import de.gupta.clean.crud.template.useCases.query.property.facade.PropertyQueryServiceFacade;
import de.gupta.commons.utility.comparison.ComparisonType;

import java.util.Collection;

public abstract class AbstractPropertyQueryApplicationController<Property, APIModelResponse>
		implements PropertyQueryApplicationController<Property, APIModelResponse>
{
	private final String propertyName;
	private final PropertyQueryServiceFacade<Property, APIModelResponse> service;

	@Override
	public Collection<APIModelResponse> queryBy(final Property propertyValue, final ComparisonType comparisonType)
	{
		return service.queryBy(propertyName, propertyValue, comparisonType);
	}

	protected AbstractPropertyQueryApplicationController(final String propertyName,
														 final PropertyQueryServiceFacade<Property, APIModelResponse> service)
	{
		this.propertyName = propertyName;
		this.service = service;
	}
}