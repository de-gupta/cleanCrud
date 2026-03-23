package de.gupta.clean.crud.template.useCases.crud.common.infrastructure.persistence.repository;

import de.gupta.clean.crud.template.infrastructure.persistence.history.adapter.TriTemporalHistorySnapshotFactory;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.JpaTriTemporalHistoryRepositoryAdapter;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryJpaRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.history.service.TriTemporalHistoryRecorder;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class AbstractHistorizedPersistenceModelJpaRepositorySupport<
		PersistenceModel extends WithID<PersistenceID>,
		PersistenceID,
		ConcretePersistenceModel extends PersistenceModel,
		HistoryModel extends TriTemporalHistoryModel<PersistenceID>>
		extends AbstractPersistenceModelJpaRepositorySupport<PersistenceModel, PersistenceID, ConcretePersistenceModel>
{
	private final TriTemporalHistoryRecorder<PersistenceID, PersistenceModel, HistoryModel> historyRecorder;

	protected final TriTemporalHistoryRecorder<PersistenceID, PersistenceModel, HistoryModel> historyRecorder()
	{
		return historyRecorder;
	}

	protected AbstractHistorizedPersistenceModelJpaRepositorySupport(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository,
			final TriTemporalHistoryRepository<PersistenceID, HistoryModel> historyRepository,
			final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory)
	{
		super(jpaRepository);
		this.historyRecorder = new TriTemporalHistoryRecorder<>(historyRepository, snapshotFactory);
	}

	protected AbstractHistorizedPersistenceModelJpaRepositorySupport(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository,
			final TriTemporalHistoryJpaRepository<PersistenceID, HistoryModel> historyRepository,
			final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory)
	{
		this(jpaRepository, new JpaTriTemporalHistoryRepositoryAdapter<>(historyRepository), snapshotFactory);
	}
}