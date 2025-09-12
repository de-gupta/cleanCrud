package de.gupta.clean.crud.template.useCases.query.specification.domain.model;

import java.util.Collection;
import java.util.function.Supplier;

public record CompositeFilterSpecification(Collection<LeafFilterSpecification> specifications,
										   CompositeFilterOperation operation)
		implements FilterSpecification
{
	public static CompositeFilterSpecification of(final Collection<LeafFilterSpecification> specifications,
												  final CompositeFilterOperation operation)
	{
		return new CompositeFilterSpecification(specifications, operation);
	}

	public static CompositeFilterSpecification with(
			final Collection<Supplier<LeafFilterSpecification>> specifications,
			final CompositeFilterOperation operation)
	{
		return of(specifications.stream().map(Supplier::get).toList(), operation);
	}

	public enum CompositeFilterOperation
	{
		AND, OR
	}
}