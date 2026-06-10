package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

@FunctionalInterface
public interface AggregateCreateValidator<DomainModel>
{
	void validate(final DomainModel domainModel);
}
