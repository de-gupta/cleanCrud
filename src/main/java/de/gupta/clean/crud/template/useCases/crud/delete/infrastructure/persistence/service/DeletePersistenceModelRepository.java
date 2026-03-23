package de.gupta.clean.crud.template.useCases.crud.delete.infrastructure.persistence.service;

import java.util.Collection;

public interface DeletePersistenceModelRepository<PersistenceID>
{
	void deleteById(final PersistenceID id);

	void deleteAllById(final Collection<PersistenceID> ids);
}