package de.gupta.clean.crud.template.useCases.crud.all.infrastructure.persistence.repository;

import de.gupta.clean.crud.template.infrastructure.persistence.history.adapter.TriTemporalHistorySnapshotFactory;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.history.service.TriTemporalHistoryRecorder;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;
import de.gupta.clean.crud.template.useCases.crud.all.infrastructure.persistence.service.PersistenceModelCrudRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;

public abstract class AbstractPersistenceModelJpaCrudRepository<PersistenceModel extends WithID<PersistenceID>,
		PersistenceID, ConcretePersistenceModel extends PersistenceModel,
		HistoryModel extends TriTemporalHistoryModel<PersistenceID>>
		implements PersistenceModelCrudRepository<PersistenceModel, PersistenceID>
{
	private final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository;
	private final TriTemporalHistoryRecorder<PersistenceID, PersistenceModel, HistoryModel> historyRecorder;

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

	@Override
	public Collection<PersistenceModel> findByIds(final Iterable<PersistenceID> ids)
	{
		return jpaRepository.findAllById(ids).stream().map(this::castUp).toList();
	}

	@Transactional
	@Override
	public PersistenceModel save(final PersistenceModel persistenceModel)
	{
		boolean existingModel = persistenceModel.id() != null && jpaRepository.existsById(persistenceModel.id());
		PersistenceModel savedModel = Optional.of(persistenceModel)
											  .flatMap(this::castDown)
											  .map(jpaRepository::save)
											  .map(this::castUp)
											  .orElseThrow(() -> new IllegalArgumentException(
													  "Could not save model: " + persistenceModel));
		if (existingModel)
		{
			historyRecorder.recordUpdate(savedModel);
		}
		else
		{
			historyRecorder.recordCreate(savedModel);
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
			existingIDs.addAll(jpaRepository.findAllById(knownIDs).stream().map(WithID::id).toList());
		}

		Collection<ConcretePersistenceModel> models =
				persistenceModels.stream()
								 .map(this::castDown)
								 .flatMap(Optional::stream)
								 .toList();
		var savedModels = jpaRepository.saveAll(models).stream().map(this::castUp).toList();
		historyRecorder.recordCreateAll(
				savedModels.stream().filter(model -> !existingIDs.contains(model.id())).toList());
		historyRecorder.recordUpdateAll(
				savedModels.stream().filter(model -> existingIDs.contains(model.id())).toList());
		return savedModels;
	}

	@Transactional
	@Override
	public void deleteById(final PersistenceID persistenceID)
	{
		jpaRepository.findById(persistenceID).map(this::castUp).ifPresent(historyRecorder::recordDelete);
		jpaRepository.deleteById(persistenceID);
	}

	@Transactional
	@Override
	public void deleteAllById(final Collection<PersistenceID> ids)
	{
		var deletedModels =
				new ArrayList<PersistenceModel>(jpaRepository.findAllById(ids).stream().map(this::castUp).toList());
		jpaRepository.deleteAllById(ids);
		historyRecorder.recordDeleteAll(deletedModels);
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
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository,
			final TriTemporalHistoryRepository<PersistenceID, HistoryModel> historyRepository,
			final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory)
	{
		this.jpaRepository = jpaRepository;
		this.historyRecorder = new TriTemporalHistoryRecorder<>(historyRepository, snapshotFactory);
	}
}