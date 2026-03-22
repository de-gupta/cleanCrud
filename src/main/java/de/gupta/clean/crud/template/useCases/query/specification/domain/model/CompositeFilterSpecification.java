package de.gupta.clean.crud.template.useCases.query.specification.domain.model;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public record CompositeFilterSpecification(Collection<FilterSpecification> specifications,
										   CompositeFilterOperation operation)
		implements FilterSpecification
{
	public static CompositeFilterSpecification of(final Collection<? extends FilterSpecification> specifications,
												  final CompositeFilterOperation operation)
	{
		return new CompositeFilterSpecification(List.copyOf(specifications), operation);
	}

	public static CompositeFilterSpecification with(
			final Collection<? extends Supplier<? extends FilterSpecification>> specifications,
			final CompositeFilterOperation operation)
	{
		return of(specifications.stream().map(Supplier::get).toList(), operation);
	}

	static FilterSpecification compose(final FilterSpecification left,
									   final FilterSpecification right,
									   final CompositeFilterOperation operation)
	{
		return switch (operation)
		{
			case AND -> composeAnd(left, right);
			case OR -> composeOr(left, right);
		};
	}

	private static FilterSpecification composeAnd(final FilterSpecification left, final FilterSpecification right)
	{
		return switch (left)
		{
			case GroupedFilterSpecification _ -> andOf(left, right);
			case CompositeFilterSpecification composite when composite.operation() == CompositeFilterOperation.OR ->
					appendToRightmostOperand(composite, right, CompositeFilterOperation.AND);
			case CompositeFilterSpecification composite when composite.operation() == CompositeFilterOperation.AND ->
					andOf(composite.specifications(), right);
			default -> andOf(left, right);
		};
	}

	private static FilterSpecification composeOr(final FilterSpecification left, final FilterSpecification right)
	{
		return orOf(left, right);
	}

	private static CompositeFilterSpecification appendToRightmostOperand(
			final CompositeFilterSpecification composite,
			final FilterSpecification right,
			final CompositeFilterOperation operation)
	{
		final List<FilterSpecification> specifications = List.copyOf(composite.specifications());
		if (specifications.isEmpty())
		{
			return of(List.of(right), composite.operation());
		}

		final int lastIndex = specifications.size() - 1;
		final FilterSpecification updatedLast = compose(specifications.get(lastIndex), right, operation);
		final List<FilterSpecification> updatedSpecifications =
				java.util.stream.IntStream.range(0, specifications.size())
										  .mapToObj(i -> i == lastIndex ? updatedLast : specifications.get(i))
										  .toList();

		return of(updatedSpecifications, composite.operation());
	}

	private static CompositeFilterSpecification andOf(final FilterSpecification left, final FilterSpecification right)
	{
		return andOf(List.of(left), right);
	}

	private static CompositeFilterSpecification andOf(final Collection<FilterSpecification> left,
													  final FilterSpecification right)
	{
		return of(flatten(left, right, CompositeFilterOperation.AND), CompositeFilterOperation.AND);
	}

	private static CompositeFilterSpecification orOf(final FilterSpecification left, final FilterSpecification right)
	{
		return of(flattenBoth(left, right, CompositeFilterOperation.OR), CompositeFilterOperation.OR);
	}

	private static List<FilterSpecification> flatten(final Collection<FilterSpecification> left,
													 final FilterSpecification right,
													 final CompositeFilterOperation operation)
	{
		return java.util.stream.Stream.concat(
						   left.stream(),
						   flattenIfMatching(right, operation).stream()
				   )
									  .toList();
	}

	private static List<FilterSpecification> flattenBoth(final FilterSpecification left,
														 final FilterSpecification right,
														 final CompositeFilterOperation operation)
	{
		return java.util.stream.Stream.concat(
						   flattenIfMatching(left, operation).stream(),
						   flattenIfMatching(right, operation).stream()
				   )
									  .toList();
	}

	private static List<FilterSpecification> flattenIfMatching(final FilterSpecification specification,
															   final CompositeFilterOperation operation)
	{
		return switch (specification)
		{
			case CompositeFilterSpecification composite
					when composite.operation() == operation -> List.copyOf(composite.specifications());
			default -> List.of(specification);
		};
	}

	public enum CompositeFilterOperation
	{
		AND, OR
	}
}