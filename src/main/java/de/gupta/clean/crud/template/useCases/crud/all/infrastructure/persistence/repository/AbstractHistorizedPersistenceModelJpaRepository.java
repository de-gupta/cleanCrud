package de.gupta.clean.crud.template.useCases.crud.all.infrastructure.persistence.repository;

import de.gupta.clean.crud.template.infrastructure.persistence.history.adapter.TriTemporalHistorySnapshotFactory;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.JpaTriTemporalHistoryRepositoryAdapter;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryJpaRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;
import de.gupta.clean.crud.template.useCases.crud.delete.infrastructure.persistence.service.DeletePersistenceModelRepository;
import de.gupta.clean.crud.template.useCases.crud.fetch.infrastructure.persistence.service.FetchPersistenceModelRepository;
import de.gupta.clean.crud.template.useCases.crud.save.infrastructure.persistence.service.SavePersistenceModelRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@Deprecated
public abstract class AbstractHistorizedPersistenceModelJpaRepository<PersistenceModel extends WithID<PersistenceID>,
		PersistenceID,
		ConcretePersistenceModel extends PersistenceModel,
		HistoryModel extends TriTemporalHistoryModel<PersistenceID>>
		extends
		AbstractPersistenceModelJpaCrudRepository<PersistenceModel, PersistenceID, ConcretePersistenceModel, HistoryModel>
		implements SavePersistenceModelRepository<PersistenceModel>,
		DeletePersistenceModelRepository<PersistenceID>,
		FetchPersistenceModelRepository<PersistenceModel, PersistenceID>
{
	protected AbstractHistorizedPersistenceModelJpaRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository,
			final TriTemporalHistoryJpaRepository<PersistenceID, HistoryModel> historyRepository,
			final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory)
	{
		super(jpaRepository, new JpaTriTemporalHistoryRepositoryAdapter<>(historyRepository), snapshotFactory);
	}
}