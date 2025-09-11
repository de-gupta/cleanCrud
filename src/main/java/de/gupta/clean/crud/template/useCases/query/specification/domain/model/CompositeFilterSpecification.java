package de.gupta.clean.crud.template.useCases.query.specification.domain.model;

import java.util.Collection;

public record CompositeFilterSpecification(Collection<LeafFilterSpecification> specifications,
										   CompositeFilterOperation operation)
		implements FilterSpecification
{
	public enum CompositeFilterOperation
	{
		AND, OR
	}
}