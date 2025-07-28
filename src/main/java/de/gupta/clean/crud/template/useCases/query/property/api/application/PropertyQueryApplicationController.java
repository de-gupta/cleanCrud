package de.gupta.clean.crud.template.useCases.query.property.api.application;

import de.gupta.commons.utility.comparison.ComparisonType;

import java.util.Collection;

public interface PropertyQueryApplicationController<Property, APIModelResponse>
{
	Collection<APIModelResponse> queryBy(final Property propertyValue, final ComparisonType comparisonType);
}