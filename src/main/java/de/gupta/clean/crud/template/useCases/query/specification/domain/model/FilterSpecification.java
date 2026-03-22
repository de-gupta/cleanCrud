package de.gupta.clean.crud.template.useCases.query.specification.domain.model;

import java.util.List;

public sealed interface FilterSpecification
		permits LeafFilterSpecification, CompositeFilterSpecification, GroupedFilterSpecification
{
	static FilterSpecification allOf(final FilterSpecification... specifications)
	{
		return CompositeFilterSpecification.of(List.of(specifications),
				CompositeFilterSpecification.CompositeFilterOperation.AND);
	}

	static FilterSpecification anyOf(final FilterSpecification... specifications)
	{
		return CompositeFilterSpecification.of(List.of(specifications),
				CompositeFilterSpecification.CompositeFilterOperation.OR);
	}

	default FilterSpecification and(final FilterSpecification other)
	{
		return CompositeFilterSpecification.compose(this, other,
				CompositeFilterSpecification.CompositeFilterOperation.AND);
	}

	default FilterSpecification or(final FilterSpecification other)
	{
		return CompositeFilterSpecification.compose(this, other,
				CompositeFilterSpecification.CompositeFilterOperation.OR);
	}

	default FilterSpecification grouped()
	{
		return GroupedFilterSpecification.of(this);
	}
}