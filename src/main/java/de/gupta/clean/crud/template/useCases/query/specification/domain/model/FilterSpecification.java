package de.gupta.clean.crud.template.useCases.query.specification.domain.model;

public sealed interface FilterSpecification permits LeafFilterSpecification, CompositeFilterSpecification
{
}