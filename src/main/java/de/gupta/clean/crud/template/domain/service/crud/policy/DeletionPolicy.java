package de.gupta.clean.crud.template.domain.service.crud.policy;

@FunctionalInterface
public interface DeletionPolicy<DomainModel>
{
	void validateDeletion(final DomainModel domainModel);
}