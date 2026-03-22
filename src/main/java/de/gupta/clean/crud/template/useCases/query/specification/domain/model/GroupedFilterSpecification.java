package de.gupta.clean.crud.template.useCases.query.specification.domain.model;

public record GroupedFilterSpecification(FilterSpecification specification) implements FilterSpecification
{
	public static GroupedFilterSpecification of(final FilterSpecification specification)
	{
		return new GroupedFilterSpecification(specification);
	}
}