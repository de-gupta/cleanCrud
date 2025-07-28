package de.gupta.clean.crud.template.useCases.query.property.facade;

import de.gupta.commons.utility.comparison.ComparisonType;

import java.util.Collection;

public interface PropertyQueryServiceFacade<Property, APIModelResponse>
{
	Collection<APIModelResponse> findAll(final String propertyName, final Property propertyValue,
										 final ComparisonType comparisonType);
}