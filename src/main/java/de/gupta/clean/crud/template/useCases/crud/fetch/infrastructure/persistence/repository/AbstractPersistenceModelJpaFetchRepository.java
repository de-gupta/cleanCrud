package de.gupta.clean.crud.template.useCases.crud.fetch.infrastructure.persistence.repository;

import de.gupta.clean.crud.template.useCases.crud.fetch.infrastructure.persistence.service.FetchPersistenceModelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public abstract class AbstractPersistenceModelJpaFetchRepository<PersistenceModel, PersistenceID, ConcretePersistenceModel extends PersistenceModel>
		implements FetchPersistenceModelRepository<PersistenceModel, PersistenceID>
{
	private final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository;

	@Override
	public Collection<PersistenceModel> findAll()
	{
		return jpaRepository.findAll().stream().map(this::castUp).toList();
	}

	@Override
	public Page<PersistenceModel> findAll(final Pageable pageable)
	{
		return jpaRepository.findAll(pageable).map(this::castUp);
	}

	@Override
	public Optional<PersistenceModel> findById(final PersistenceID persistenceID)
	{
		return jpaRepository.findById(persistenceID).map(this::castUp);
	}

	@Override
	public Collection<PersistenceModel> findByIds(final Iterable<PersistenceID> IDs)
	{
		return jpaRepository.findAllById(IDs).stream().map(this::castUp).toList();
	}

	private PersistenceModel castUp(final ConcretePersistenceModel persistenceModel)
	{
		return persistenceModel;
	}

	protected AbstractPersistenceModelJpaFetchRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository)
	{
		this.jpaRepository = jpaRepository;
	}
}