package de.gupta.clean.crud.template.useCases.crud.update.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

import java.util.Optional;

public interface UpdatePersistenceService<DomainID, DomainModel>
{
	void putAtId(final DomainID id, final DomainModel model);

	IdentifiedModel<DomainID, DomainModel> updateById(final DomainID id, final DomainModel model);

	Optional<IdentifiedModel<DomainID, DomainModel>> findById(final DomainID id);
}