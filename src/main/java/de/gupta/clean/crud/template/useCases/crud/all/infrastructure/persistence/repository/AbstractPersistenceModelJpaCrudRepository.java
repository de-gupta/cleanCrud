package de.gupta.clean.crud.template.useCases.crud.all.infrastructure.persistence.repository;

import de.gupta.clean.crud.template.useCases.crud.all.infrastructure.persistence.service.PersistenceModelCrudRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public abstract class AbstractPersistenceModelJpaCrudRepository<PersistenceModel, PersistenceID, ConcretePersistenceModel extends PersistenceModel>
		implements PersistenceModelCrudRepository<PersistenceModel, PersistenceID>
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
	public boolean existsById(final PersistenceID persistenceID)
	{
		return jpaRepository.existsById(persistenceID);
	}

	@Override
	public Optional<PersistenceModel> findById(final PersistenceID persistenceID)
	{
		return jpaRepository.findById(persistenceID).map(this::castUp);
	}

	@Transactional
	@Override
	public PersistenceModel save(final PersistenceModel persistenceModel)
	{
		return Optional.of(persistenceModel)
					   .flatMap(this::castDown)
					   .map(jpaRepository::save)
					   .orElseThrow(() -> new IllegalArgumentException("Could not save model: " + persistenceModel));
	}

	@Transactional
	@Override
	public Collection<PersistenceModel> saveAll(final Collection<PersistenceModel> persistenceModels)
	{
		Collection<ConcretePersistenceModel> models =
				persistenceModels.stream()
								 .map(this::castDown)
								 .flatMap(Optional::stream)
								 .toList();
		return jpaRepository.saveAll(models).stream().map(this::castUp).toList();
	}

	@Transactional
	@Override
	public void deleteById(final PersistenceID persistenceID)
	{
		jpaRepository.deleteById(persistenceID);
	}

	@SuppressWarnings("unchecked")
	private Optional<ConcretePersistenceModel> castDown(final PersistenceModel model)
	{
		try
		{
			return Optional.of((ConcretePersistenceModel) model);
		}
		catch (ClassCastException e)
		{
			return Optional.empty();
		}
	}

	private PersistenceModel castUp(final ConcretePersistenceModel persistenceModel)
	{
		return persistenceModel;
	}

	protected AbstractPersistenceModelJpaCrudRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository)
	{
		this.jpaRepository = jpaRepository;
	}
}