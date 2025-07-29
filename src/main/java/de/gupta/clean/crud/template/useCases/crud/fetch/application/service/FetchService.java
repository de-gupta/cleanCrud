package de.gupta.clean.crud.template.useCases.crud.fetch.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Set;

public interface FetchService<DomainModel, DomainID>
{
	Collection<IdentifiedModel<DomainID, DomainModel>> findAll();

	Page<IdentifiedModel<DomainID, DomainModel>> findAll(final Pageable pageable);

	IdentifiedModel<DomainID, DomainModel> findById(final DomainID id);

	Collection<IdentifiedModel<DomainID, DomainModel>> findByIds(final Set<DomainID> IDs);
}