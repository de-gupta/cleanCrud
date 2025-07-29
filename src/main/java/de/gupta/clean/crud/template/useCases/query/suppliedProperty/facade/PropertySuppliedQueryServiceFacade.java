package de.gupta.clean.crud.template.useCases.query.suppliedProperty.facade;

import de.gupta.commons.utility.comparison.ComparisonType;

import java.util.Collection;

public interface PropertySuppliedQueryServiceFacade<APIModelResponse>
{
	Collection<APIModelResponse> queryBySuppliedProperty(final String propertyName,
														 final ComparisonType comparisonType);
}