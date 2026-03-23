package de.gupta.clean.crud.template.useCases.crud.update.infrastructure.persistence.service;

import java.util.Collection;

public interface UpdatePersistenceModelRepository<PersistenceModel>
{
	PersistenceModel update(PersistenceModel model);

	Collection<PersistenceModel> updateAll(Collection<PersistenceModel> models);
}