package de.gupta.clean.crud.template.useCases.crud.fetch.infrastructure.persistence.service;

import de.gupta.clean.crud.template.specification.ModelSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;

public interface FetchPersistenceModelRepository<PersistenceModel, PersistenceID>
{
	Collection<PersistenceModel> findAll();

	Collection<PersistenceModel> findAll(final ModelSpecification<PersistenceModel> filter);

	Page<PersistenceModel> findAll(final Pageable pageable);

	Page<PersistenceModel> findAll(final ModelSpecification<PersistenceModel> specification, final Pageable pageable);

	Optional<PersistenceModel> findById(final PersistenceID id);

	Collection<PersistenceModel> findByIds(final Iterable<PersistenceID> IDs);
}