package de.gupta.clean.crud.template.useCases.query.specification.domain.model;

import de.gupta.commons.utility.comparison.ComparisonType;

public record PropertyFilterSpecification<Property>(String propertyName, Property propertyValue,
													ComparisonType comparisonType)
		implements LeafFilterSpecification
{
	public static <P> PropertyFilterSpecification<P> with(final String propertyName, final P propertyValue,
														  final ComparisonType comparisonType)
	{
		return new PropertyFilterSpecification<>(propertyName, propertyValue, comparisonType);
	}
}