package de.gupta.clean.crud.template.useCases.crud.save.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

import java.util.Collection;

public interface SavePersistenceService<DomainID, DomainModel>
{
	IdentifiedModel<DomainID, DomainModel> save(final DomainModel model);

	Collection<IdentifiedModel<DomainID, DomainModel>> saveAll(final Collection<DomainModel> models);
}