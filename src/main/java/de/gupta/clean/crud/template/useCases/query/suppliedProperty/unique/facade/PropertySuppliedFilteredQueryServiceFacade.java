package de.gupta.clean.crud.template.useCases.query.suppliedProperty.unique.facade;

import de.gupta.commons.utility.comparison.ComparisonType;

import java.util.Optional;

public interface PropertySuppliedFilteredQueryServiceFacade<APIModelResponse>
{
	Optional<APIModelResponse> queryBySuppliedPropertyAndFilter(final String propertyName,
																final ComparisonType comparisonType,
																final Object propertyValue);
}