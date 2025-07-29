package de.gupta.clean.crud.template.useCases.query.suppliedProperty.api.application;

import de.gupta.commons.utility.comparison.ComparisonType;

import java.util.Collection;

public interface PropertySuppliedQueryApplicationController<APIModelResponse>
{
	Collection<APIModelResponse> queryBy(final String propertyName,
										 final ComparisonType comparisonType);
}