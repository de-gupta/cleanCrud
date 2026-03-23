package de.gupta.clean.crud.template.useCases.crud.delete.infrastructure.persistence.repository;

import de.gupta.clean.crud.template.infrastructure.persistence.history.adapter.TriTemporalHistorySnapshotFactory;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.history.service.TriTemporalHistoryRecorder;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;
import de.gupta.clean.crud.template.useCases.crud.delete.infrastructure.persistence.service.DeletePersistenceModelRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public abstract class AbstractPersistenceModelJpaDeleteRepository<PersistenceModel extends WithID<PersistenceID>,
		PersistenceID, ConcretePersistenceModel extends PersistenceModel,
		HistoryModel extends TriTemporalHistoryModel<PersistenceID>>
		implements DeletePersistenceModelRepository<PersistenceID>
{
	private final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository;
	private final TriTemporalHistoryRecorder<PersistenceID, PersistenceModel, HistoryModel> historyRecorder;

	@Override
	public void deleteById(final PersistenceID persistenceID)
	{
		jpaRepository.findById(persistenceID).map(this::castUp).ifPresent(historyRecorder::recordDelete);
		jpaRepository.deleteById(persistenceID);
	}

	@Override
	public void deleteAllById(final Collection<PersistenceID> ids)
	{
		historyRecorder.recordDeleteAll(jpaRepository.findAllById(ids).stream().map(this::castUp).toList());
		jpaRepository.deleteAllById(ids);
	}

	private PersistenceModel castUp(final ConcretePersistenceModel persistenceModel)
	{
		return persistenceModel;
	}

	protected AbstractPersistenceModelJpaDeleteRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository,
			final TriTemporalHistoryRepository<PersistenceID, HistoryModel> historyRepository,
			final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory)
	{
		this.jpaRepository = jpaRepository;
		this.historyRecorder = new TriTemporalHistoryRecorder<>(historyRepository, snapshotFactory);
	}
}