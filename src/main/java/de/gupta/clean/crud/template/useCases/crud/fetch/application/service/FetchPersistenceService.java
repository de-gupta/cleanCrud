package de.gupta.clean.crud.template.useCases.crud.fetch.application.service;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.specification.ModelSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface FetchPersistenceService<DomainID, DomainModel>
{
	Collection<IdentifiedModel<DomainID, DomainModel>> findAll();

	Collection<IdentifiedModel<DomainID, DomainModel>> findAll(final ModelSpecification<DomainModel> specification);

	Page<IdentifiedModel<DomainID, DomainModel>> findAll(final Pageable pageable);

	Page<IdentifiedModel<DomainID, DomainModel>> findAll(final ModelSpecification<DomainModel> specification,
														 final Pageable pageable);

	Optional<IdentifiedModel<DomainID, DomainModel>> findById(final DomainID id);

	Collection<IdentifiedModel<DomainID, DomainModel>> findByIds(final Set<DomainID> IDs);
}