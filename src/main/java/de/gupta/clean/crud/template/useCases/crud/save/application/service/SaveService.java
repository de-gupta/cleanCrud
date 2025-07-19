package de.gupta.clean.crud.template.useCases.crud.save.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

import java.util.Collection;

public interface SaveService<DomainModelCreate, DomainModelResponse, DomainID>
{
	IdentifiedModel<DomainID, DomainModelResponse> save(final DomainModelCreate model);

	Collection<IdentifiedModel<DomainID, DomainModelResponse>> saveAll(final Collection<DomainModelCreate> models);
}