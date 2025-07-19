package de.gupta.clean.crud.template.domain.service.crud.policy;

@FunctionalInterface
public interface InsertionPolicy<DomainModel>
{
	void validateInsertion(final DomainModel domainModel);
}