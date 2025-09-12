package de.gupta.clean.crud.template.useCases.query.specification.domain.model;

import de.gupta.commons.utility.comparison.ComparisonType;

import java.util.function.Supplier;

public record SuppliedPropertyFilterSpecification<Property>(String propertyName, Supplier<Property> propertyValue,
															ComparisonType comparisonType)
		implements LeafFilterSpecification
{
	public static <P> SuppliedPropertyFilterSpecification<P> of(final String propertyName,
																final Supplier<P> propertyValue,
																final ComparisonType comparisonType)
	{
		return new SuppliedPropertyFilterSpecification<>(propertyName, propertyValue, comparisonType);
	}
}