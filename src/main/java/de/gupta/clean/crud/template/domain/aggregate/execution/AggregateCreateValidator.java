package de.gupta.clean.crud.template.domain.aggregate.execution;

@FunctionalInterface
public interface AggregateCreateValidator<DomainModel>
{
	void validate(final DomainModel domainModel);
}