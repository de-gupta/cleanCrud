package de.gupta.clean.crud.template.useCases.crud.fetch.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Set;

public interface FetchService<DomainModelResponse, DomainID>
{
	Collection<IdentifiedModel<DomainID, DomainModelResponse>> findAll();

	Page<IdentifiedModel<DomainID, DomainModelResponse>> findAll(final Pageable pageable);

	IdentifiedModel<DomainID, DomainModelResponse> findById(final DomainID id);

	Collection<IdentifiedModel<DomainID, DomainModelResponse>> findByIds(final Set<DomainID> IDs);
}