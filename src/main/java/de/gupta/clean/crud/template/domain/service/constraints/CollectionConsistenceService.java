package de.gupta.clean.crud.template.domain.service.constraints;

@FunctionalInterface
@Deprecated
public interface CollectionConsistenceService<DomainModel>
{
	void validateCollection(final Iterable<DomainModel> collection);
}