package de.gupta.clean.crud.template.useCases.query.property.api.application;

import de.gupta.clean.crud.template.useCases.query.property.facade.PropertyQueryServiceFacade;
import de.gupta.commons.utility.comparison.ComparisonType;

import java.util.Collection;

public abstract class AbstractPropertyQueryApplicationController<Property, APIModelResponse>
		implements PropertyQueryApplicationController<Property, APIModelResponse>
{
	private final PropertyQueryServiceFacade<Property, APIModelResponse> service;

	@Override
	public Collection<APIModelResponse> queryBy(final String propertyName, final Property propertyValue,
												final ComparisonType comparisonType)
	{
		return service.queryBy(propertyName, propertyValue, comparisonType);
	}

	protected AbstractPropertyQueryApplicationController(
			final PropertyQueryServiceFacade<Property, APIModelResponse> service)
	{
		this.service = service;
	}
}