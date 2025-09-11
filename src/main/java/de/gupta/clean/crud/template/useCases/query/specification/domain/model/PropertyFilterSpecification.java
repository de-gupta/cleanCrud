package de.gupta.clean.crud.template.useCases.query.specification.domain.model;

import de.gupta.commons.utility.comparison.ComparisonType;

import java.util.function.Supplier;

public record PropertyFilterSpecification<Property>(String propertyName, Supplier<Property> propertyValue,
													ComparisonType comparisonType)
		implements LeafFilterSpecification
{
	public static <P> PropertyFilterSpecification<P> of(final String propertyName, final Supplier<P> propertyValue,
														final ComparisonType comparisonType)
	{
		return new PropertyFilterSpecification<>(propertyName, propertyValue, comparisonType);
	}
}