package de.gupta.clean.crud.template.useCases.crud.save.infrastructure.persistence.repository;

import de.gupta.clean.crud.template.useCases.crud.save.infrastructure.persistence.service.SavePersistenceModelRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public abstract class AbstractPersistenceModelJpaSaveRepository<PersistenceModel, PersistenceID, ConcretePersistenceModel extends PersistenceModel>
		implements SavePersistenceModelRepository<PersistenceModel>
{
	private final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository;

	@Override
	public PersistenceModel save(final PersistenceModel model)
	{
		return castUp(jpaRepository.save(castDown(model)));
	}

	@Override
	public Collection<PersistenceModel> saveAll(final Collection<PersistenceModel> models)
	{
		return jpaRepository.saveAll(models.stream().map(this::castDown).toList())
							.stream().map(this::castUp).toList();
	}

	private PersistenceModel castUp(final ConcretePersistenceModel persistenceModel)
	{
		return persistenceModel;
	}

	@SuppressWarnings("unchecked")
	private ConcretePersistenceModel castDown(final PersistenceModel persistenceModel)
	{
		return (ConcretePersistenceModel) persistenceModel;
	}

	protected AbstractPersistenceModelJpaSaveRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository)
	{
		this.jpaRepository = jpaRepository;
	}
}