package de.gupta.clean.crud.template.useCases.crud.update.infrastructure.persistence.repository;

import de.gupta.clean.crud.template.infrastructure.persistence.history.adapter.TriTemporalHistorySnapshotFactory;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryJpaRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;
import de.gupta.clean.crud.template.useCases.crud.common.infrastructure.persistence.repository.AbstractHistorizedPersistenceModelJpaRepositorySupport;
import de.gupta.clean.crud.template.useCases.crud.update.infrastructure.persistence.service.UpdatePersistenceModelRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public abstract class AbstractPersistenceModelJpaUpdateRepository<PersistenceModel extends WithID<PersistenceID>,
		PersistenceID, ConcretePersistenceModel extends PersistenceModel,
		HistoryModel extends TriTemporalHistoryModel<PersistenceID>>
		extends AbstractHistorizedPersistenceModelJpaRepositorySupport<PersistenceModel, PersistenceID,
		ConcretePersistenceModel, HistoryModel>
		implements UpdatePersistenceModelRepository<PersistenceModel>
{
	@Override
	public PersistenceModel update(final PersistenceModel model)
	{
		PersistenceModel updatedModel = castUp(jpaRepository().save(castDown(model)));
		historyRecorder().recordUpdate(updatedModel);
		return updatedModel;
	}

	@Override
	public Collection<PersistenceModel> updateAll(final Collection<PersistenceModel> models)
	{
		var updatedModels = jpaRepository().saveAll(models.stream().map(this::castDown).toList()).stream().map(
				this::castUp).toList();
		historyRecorder().recordUpdateAll(updatedModels);
		return updatedModels;
	}

	protected AbstractPersistenceModelJpaUpdateRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository,
			final TriTemporalHistoryRepository<PersistenceID, HistoryModel> historyRepository,
			final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory)
	{
		super(jpaRepository, historyRepository, snapshotFactory);
	}

	protected AbstractPersistenceModelJpaUpdateRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository,
			final TriTemporalHistoryJpaRepository<PersistenceID, HistoryModel> historyRepository,
			final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory)
	{
		super(jpaRepository, historyRepository, snapshotFactory);
	}
}