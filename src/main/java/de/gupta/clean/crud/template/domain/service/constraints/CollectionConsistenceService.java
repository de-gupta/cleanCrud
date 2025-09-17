package de.gupta.clean.crud.template.domain.service.constraints;

@FunctionalInterface
public interface CollectionConsistenceService<DomainModel>
{
	void isThisCollectionConsistent(final Iterable<DomainModel> collection);
}