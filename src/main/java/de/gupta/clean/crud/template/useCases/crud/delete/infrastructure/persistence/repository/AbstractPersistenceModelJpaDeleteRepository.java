package de.gupta.clean.crud.template.useCases.crud.delete.infrastructure.persistence.repository;

import de.gupta.clean.crud.template.useCases.crud.delete.infrastructure.persistence.service.DeletePersistenceModelRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public abstract class AbstractPersistenceModelJpaDeleteRepository<PersistenceModel, PersistenceID,
		ConcretePersistenceModel extends PersistenceModel>
		implements DeletePersistenceModelRepository<PersistenceID>
{
	private final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository;

	@Override
	public void deleteById(final PersistenceID persistenceID)
	{
		jpaRepository.deleteById(persistenceID);
	}

	@Override
	public void deleteAllById(final Collection<PersistenceID> ids)
	{
		jpaRepository.deleteAllById(ids);
	}

	protected AbstractPersistenceModelJpaDeleteRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository)
	{
		this.jpaRepository = jpaRepository;
	}
}