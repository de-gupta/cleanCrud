package de.gupta.clean.crud.template.useCases.query.specification.domain.model;

import de.gupta.commons.utility.comparison.ComparisonType;

public record PropertyFilterSpecification<Property>(Property propertyValue, ComparisonType comparisonType)
		implements LeafFilterSpecification
{
}