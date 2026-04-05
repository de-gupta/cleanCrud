package de.gupta.clean.crud.template.useCases.crud.delete.infrastructure.persistence.repository;

import de.gupta.clean.crud.template.infrastructure.persistence.history.adapter.TriTemporalHistorySnapshotFactory;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryJpaRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.history.service.AuditActorSupplier;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;
import de.gupta.clean.crud.template.useCases.crud.common.infrastructure.persistence.repository.AbstractHistorizedPersistenceModelJpaRepositorySupport;
import de.gupta.clean.crud.template.useCases.crud.delete.infrastructure.persistence.service.DeletePersistenceModelRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public abstract class AbstractPersistenceModelJpaDeleteRepository<PersistenceModel extends WithID<PersistenceID>,
		PersistenceID, ConcretePersistenceModel extends PersistenceModel,
		HistoryModel extends TriTemporalHistoryModel<PersistenceID>>
		extends AbstractHistorizedPersistenceModelJpaRepositorySupport<PersistenceModel, PersistenceID,
		ConcretePersistenceModel, HistoryModel>
		implements DeletePersistenceModelRepository<PersistenceID>
{
	@Override
	public void deleteById(final PersistenceID persistenceID)
	{
		jpaRepository().findById(persistenceID).map(this::castUp).ifPresent(historyRecorder()::recordDelete);
		jpaRepository().deleteById(persistenceID);
	}

	@Override
	public void deleteAllById(final Collection<PersistenceID> ids)
	{
		historyRecorder().recordDeleteAll(jpaRepository().findAllById(ids).stream().map(this::castUp).toList());
		jpaRepository().deleteAllById(ids);
	}

	protected AbstractPersistenceModelJpaDeleteRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository,
			final TriTemporalHistoryRepository<PersistenceID, HistoryModel> historyRepository,
			final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory,
			final AuditActorSupplier auditActorSupplier)
	{
		super(jpaRepository, historyRepository, snapshotFactory, auditActorSupplier);
	}

	protected AbstractPersistenceModelJpaDeleteRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository,
			final TriTemporalHistoryJpaRepository<PersistenceID, HistoryModel> historyRepository,
			final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory,
			final AuditActorSupplier auditActorSupplier)
	{
		super(jpaRepository, historyRepository, snapshotFactory, auditActorSupplier);
	}
}