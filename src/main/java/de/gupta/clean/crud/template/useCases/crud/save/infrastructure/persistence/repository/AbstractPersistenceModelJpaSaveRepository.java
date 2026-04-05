package de.gupta.clean.crud.template.useCases.crud.save.infrastructure.persistence.repository;

import de.gupta.clean.crud.template.infrastructure.persistence.history.adapter.TriTemporalHistorySnapshotFactory;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryJpaRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.history.service.AuditActorSupplier;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;
import de.gupta.clean.crud.template.useCases.crud.common.infrastructure.persistence.repository.AbstractHistorizedPersistenceModelJpaRepositorySupport;
import de.gupta.clean.crud.template.useCases.crud.save.infrastructure.persistence.service.SavePersistenceModelRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.LinkedHashSet;

public abstract class AbstractPersistenceModelJpaSaveRepository<PersistenceModel extends WithID<PersistenceID>,
		PersistenceID, ConcretePersistenceModel extends PersistenceModel,
		HistoryModel extends TriTemporalHistoryModel<PersistenceID>>
		extends AbstractHistorizedPersistenceModelJpaRepositorySupport<PersistenceModel, PersistenceID,
		ConcretePersistenceModel, HistoryModel>
		implements SavePersistenceModelRepository<PersistenceModel>
{
	@Override
	public PersistenceModel save(final PersistenceModel model)
	{
		boolean existingModel = model.id() != null && jpaRepository().existsById(model.id());
		PersistenceModel savedModel = castUp(jpaRepository().save(castDown(model)));
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

	@Override
	public Collection<PersistenceModel> saveAll(final Collection<PersistenceModel> models)
	{
		var existingIDs = new LinkedHashSet<PersistenceID>();
		var knownIDs = models.stream().map(WithID::id).filter(java.util.Objects::nonNull).toList();
		if (!knownIDs.isEmpty())
		{
			existingIDs.addAll(jpaRepository().findAllById(knownIDs).stream().map(WithID::id).toList());
		}

		var savedModels = jpaRepository().saveAll(models.stream().map(this::castDown).toList()).stream().map(
				this::castUp).toList();

		var createdModels = savedModels.stream().filter(model -> !existingIDs.contains(model.id())).toList();
		var updatedModels = savedModels.stream().filter(model -> existingIDs.contains(model.id())).toList();
		historyRecorder().recordCreateAll(createdModels);
		historyRecorder().recordUpdateAll(updatedModels);
		return savedModels;
	}

	protected AbstractPersistenceModelJpaSaveRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository,
			final TriTemporalHistoryRepository<PersistenceID, HistoryModel> historyRepository,
			final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory,
			final AuditActorSupplier auditActorSupplier)
	{
		super(jpaRepository, historyRepository, snapshotFactory, auditActorSupplier);
	}

	protected AbstractPersistenceModelJpaSaveRepository(
			final JpaRepository<ConcretePersistenceModel, PersistenceID> jpaRepository,
			final TriTemporalHistoryJpaRepository<PersistenceID, HistoryModel> historyRepository,
			final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory,
			final AuditActorSupplier auditActorSupplier)
	{
		super(jpaRepository, historyRepository, snapshotFactory, auditActorSupplier);
	}
}