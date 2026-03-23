package de.gupta.clean.crud.template.useCases.crud.update.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

import java.util.Collection;

public interface UpdatePersistenceService<DomainID, DomainModel>
{
	void putAtId(final DomainID id, final DomainModel model);

	IdentifiedModel<DomainID, DomainModel> updateById(final DomainID id, final DomainModel model);

	Collection<IdentifiedModel<DomainID, DomainModel>> updateAllById(
			final Collection<IdentifiedModel<DomainID, DomainModel>> models);
}