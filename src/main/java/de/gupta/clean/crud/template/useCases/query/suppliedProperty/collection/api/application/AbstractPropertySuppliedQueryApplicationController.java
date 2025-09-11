package de.gupta.clean.crud.template.useCases.query.suppliedProperty.collection.api.application;

import de.gupta.clean.crud.template.useCases.query.suppliedProperty.collection.facade.PropertySuppliedQueryServiceFacade;
import de.gupta.commons.utility.comparison.ComparisonType;

import java.util.Collection;

public abstract class AbstractPropertySuppliedQueryApplicationController<APIModelResponse>
		implements PropertySuppliedQueryApplicationController<APIModelResponse>
{
	private final PropertySuppliedQueryServiceFacade<APIModelResponse> service;

	@Override
	public Collection<APIModelResponse> queryBy(final String propertyName,
												final ComparisonType comparisonType)
	{
		return service.queryBySuppliedProperty(propertyName, comparisonType);
	}

	protected AbstractPropertySuppliedQueryApplicationController(
			final PropertySuppliedQueryServiceFacade<APIModelResponse> service)
	{
		this.service = service;
	}
}