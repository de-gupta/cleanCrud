package de.gupta.clean.crud.template.useCases.query.specification.domain.model;

import java.util.Collection;

public record CompositeFilterSpecification(Collection<LeafFilterSpecification> specifications,
										   CompositeFilterOperation operation)
		implements FilterSpecification
{
	public static CompositeFilterSpecification of(final Collection<LeafFilterSpecification> specifications,
												  final CompositeFilterOperation operation)
	{
		return new CompositeFilterSpecification(specifications, operation);
	}

	public enum CompositeFilterOperation
	{
		AND, OR
	}
}