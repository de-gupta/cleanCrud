package de.gupta.clean.crud.template.useCases.crud.update.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

public interface UpdateService<DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse, DomainID>
{
	void putAtId(final DomainID id, final DomainModelCreate model);

	IdentifiedModel<DomainID, DomainModelResponse> updateById(final DomainID id, final DomainModelUpdatePatch model);
}