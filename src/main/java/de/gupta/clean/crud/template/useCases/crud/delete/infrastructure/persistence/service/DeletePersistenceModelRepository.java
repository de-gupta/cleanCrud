package de.gupta.clean.crud.template.useCases.crud.delete.infrastructure.persistence.service;

public interface DeletePersistenceModelRepository<PersistenceID>
{
	void deleteById(final PersistenceID id);
}