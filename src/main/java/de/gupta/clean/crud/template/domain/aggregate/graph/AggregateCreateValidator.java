package de.gupta.clean.crud.template.domain.aggregate.graph;

@FunctionalInterface
public interface AggregateCreateValidator<DomainModel>
{
	void validate(final DomainModel domainModel);
}