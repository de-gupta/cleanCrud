package de.gupta.clean.crud.template.domain.service.aggregate.mechanics.validator;

@FunctionalInterface
public interface AggregateCreateValidator<DomainModel>
{
	void validate(final DomainModel domainModel);
}