package de.gupta.clean.crud.template.useCases.crud.all.infrastructure.persistence.repository;

import de.gupta.clean.crud.template.infrastructure.persistence.history.adapter.TriTemporalHistorySnapshotFactory;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryJpaRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;
import de.gupta.clean.crud.template.useCases.crud.all.infrastructure.persistence.service.PersistenceModelCrudRepository;
import de.gupta.clean.crud.template.useCases.crud.common.infrastructure.persistence.repository.AbstractHistorizedPersistenceModelJpaRepositorySupport;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;

@Deprecated
public abstract class AbstractPersistenceModelJpaCrudRepository<PersistenceModel extends WithID<PersistenceID>,
		PersistenceID, ConcretePersistenceModel extends PersistenceModel,
		HistoryModel extends TriTemporalHistoryModel<PersistenceID>>
		extends AbstractHistorizedPersistenceModelJpaRepositorySupport<PersistenceModel, PersistenceID,
		ConcretePersistenceModel, HistoryModel>
		implements PersistenceModelCrudRepository<PersistenceModel, PersistenceID>
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
	public boolean existsById(final PersistenceID persistenceID)
	{
		return jpaRepository().existsById(persistenceID);
	}

	@Override
	public Optional<PersistenceModel> findById(final PersistenceID persistenceID)
	{
		return jpaRepository().findById(persistenceID).map(this::castUp);
	}

	@Override
	public Collection<PersistenceModel> findByIds(final Iterable<PersistenceID> ids)
	{
		return jpaRepository().findAllById(ids).stream().map(this::castUp).toList();
	}

	@Transactional
	@Override
	public PersistenceModel save(final PersistenceModel persistenceModel)
	{
		boolean existingModel = persistenceModel.id() != null && jpaRepository().existsById(persistenceModel.id());
		PersistenceModel savedModel = castUp(jpaRepository().save(castDown(persistenceModel)));
		if (existingModel)
		{
			historyRecorder().recordUpdate(savedModel);
		}
		else
		{
			historyRecorder().recordCreate(savedModel);
		}
		return savedModel;
	}

	@Transactional
	@Override
	public Collection<PersistenceModel> saveAll(final Collection<PersistenceModel> persistenceModels)
	{
		var existingIDs = new LinkedHashSet<PersistenceID>();
		var knownIDs = persistenceModels.stream().map(WithID::id).filter(java.util.Objects::nonNull).toList();
		if (!knownIDs.isEmpty())
		{
			existingIDs.addAll(jpaRepository().findAllById(knownIDs).stream().map(WithID::id).toList());
		}

		Collection<ConcretePersistenceModel> models =
				persistenceModels.stream()
								 .map(this::castDown)
								 .toList();
		var savedModels = jpaRepository().saveAll(models).stream().map(this::castUp).toList();
		historyRecorder().recordCreateAll(
				savedModels.stream().filter(model -> !existingIDs.contains(model.id())).toList());
		historyRecorder().recordUpdateAll(
				savedModels.stream().filter(model -> existingIDs.contains(model.id())).toList());
		return savedModels;
	}

	@Transactional
	@Override
	public void deleteById(final PersistenceID persistenceID)
	{
		jpaRepository().findById(persistenceID).map(this::castUp).ifPresent(historyRecorder()::recordDelete);
		jpaRepository().deleteById(persistenceID);
	}

	@Transactional
	@Override
	public void deleteAllById(final Collection<PersistenceID> ids)
	{
		var deletedModels =
				new ArrayList<PersistenceModel>(jpaRepository().findAllById(ids).stream().map(this::castUp).toList());
		jpaRepository().deleteAllById(ids);
		historyRecorder().recordDeleteAll(deletedModels);
	}

	protected AbstractPersistenceModelJpaCrudRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository,
			final TriTemporalHistoryRepository<PersistenceID, HistoryModel> historyRepository,
			final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory)
	{
		super(jpaRepository, historyRepository, snapshotFactory);
	}

	protected AbstractPersistenceModelJpaCrudRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository,
			final TriTemporalHistoryJpaRepository<PersistenceID, HistoryModel> historyRepository,
			final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory)
	{
		super(jpaRepository, historyRepository, snapshotFactory);
	}
}