package de.gupta.clean.crud.template.useCases.crud.fetch.infrastructure.persistence.repository;

import de.gupta.clean.crud.template.useCases.crud.common.infrastructure.persistence.repository.AbstractPersistenceModelJpaRepositorySupport;
import de.gupta.clean.crud.template.useCases.crud.fetch.infrastructure.persistence.service.FetchPersistenceModelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public abstract class AbstractPersistenceModelJpaFetchRepository<PersistenceModel, PersistenceID, ConcretePersistenceModel extends PersistenceModel>
		extends AbstractPersistenceModelJpaRepositorySupport<PersistenceModel, PersistenceID, ConcretePersistenceModel>
		implements FetchPersistenceModelRepository<PersistenceModel, PersistenceID>
{
	@Override
	public Collection<PersistenceModel> findAll()
	{
		return jpaRepository().findAll().stream().map(this::castUp).toList();
	}

	@Override
	public Page<PersistenceModel> findAll(final Pageable pageable)
	{
		return jpaRepository().findAll(pageable).map(this::castUp);
	}

	@Override
	public Optional<PersistenceModel> findById(final PersistenceID persistenceID)
	{
		return jpaRepository().findById(persistenceID).map(this::castUp);
	}

	@Override
	public Collection<PersistenceModel> findByIds(final Iterable<PersistenceID> IDs)
	{
		return jpaRepository().findAllById(IDs).stream().map(this::castUp).toList();
	}

	protected AbstractPersistenceModelJpaFetchRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository)
	{
		super(jpaRepository);
	}
}