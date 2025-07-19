package de.gupta.clean.crud.template.useCases.crud.all.infrastructure.persistence.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;

public interface PersistenceModelCrudRepository<PersistenceModel, PersistenceID>
{
	Collection<PersistenceModel> findAll();

	Page<PersistenceModel> findAll(final Pageable pageable);

	boolean existsById(final PersistenceID id);

	Optional<PersistenceModel> findById(final PersistenceID id);

	PersistenceModel save(final PersistenceModel model);

	Collection<PersistenceModel> saveAll(final Collection<PersistenceModel> models);

	void deleteById(final PersistenceID id);
}