package de.gupta.clean.crud.template.useCases.crud.save.infrastructure.persistence.service;

import java.util.Collection;

public interface SavePersistenceModelRepository<PersistenceModel>
{
	PersistenceModel save(final PersistenceModel model);

	Collection<PersistenceModel> saveAll(final Collection<PersistenceModel> models);
}