package de.gupta.clean.crud.template.useCases.crud.common.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public abstract class AbstractPersistenceModelJpaRepositorySupport<PersistenceModel, PersistenceID,
		ConcretePersistenceModel extends PersistenceModel>
{
	private final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository;

	@SuppressWarnings("unchecked")
	protected final ConcretePersistenceModel castDown(final PersistenceModel persistenceModel)
	{
		return (ConcretePersistenceModel) persistenceModel;
	}

	protected final PersistenceModel castUp(final ConcretePersistenceModel persistenceModel)
	{
		return persistenceModel;
	}

	protected final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository()
	{
		return jpaRepository;
	}

	protected AbstractPersistenceModelJpaRepositorySupport(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository)
	{
		this.jpaRepository = jpaRepository;
	}
}