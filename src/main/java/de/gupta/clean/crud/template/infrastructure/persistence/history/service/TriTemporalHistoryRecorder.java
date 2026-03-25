package de.gupta.clean.crud.template.infrastructure.persistence.history.service;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceStateConflictException;
import de.gupta.clean.crud.template.infrastructure.persistence.history.adapter.TriTemporalHistorySnapshotFactory;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalChangeType;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalValidity;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TriTemporalHistoryRecorder<PersistenceID, PersistenceModel extends WithID<PersistenceID>,
		HistoryModel extends TriTemporalHistoryModel<PersistenceID>>
{
	private final TriTemporalHistoryRepository<PersistenceID, HistoryModel> repository;
	private final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory;

	public TriTemporalHistoryRecorder(
			final TriTemporalHistoryRepository<PersistenceID, HistoryModel> repository,
			final TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel> snapshotFactory)
	{
		this.repository = repository;
		this.snapshotFactory = snapshotFactory;
	}

	public void recordCreate(final PersistenceModel model)
	{
		recordCreateAll(java.util.List.of(model));
	}

	public void recordCreateAll(final Collection<? extends PersistenceModel> models)
	{
		if (models.isEmpty())
		{
			return;
		}

		Instant now = Instant.now();
		repository.saveAll(models.stream()
								 .map(model -> snapshotFactory.createSnapshot(model, TemporalChangeType.CREATED, now,
										 now,
										 TemporalValidity.defaultEndValidity()))
								 .toList());
	}

	public void recordUpdate(final PersistenceModel model)
	{
		recordUpdateAll(java.util.List.of(model));
	}

	public void recordUpdateAll(final Collection<? extends PersistenceModel> models)
	{
		recordChangeAll(models, TemporalChangeType.UPDATED);
	}

	public void recordDelete(final PersistenceModel model)
	{
		recordDeleteAll(java.util.List.of(model));
	}

	public void recordDeleteAll(final Collection<? extends PersistenceModel> models)
	{
		recordChangeAll(models, TemporalChangeType.DELETED);
	}

	private void recordChangeAll(
			final Collection<? extends PersistenceModel> models,
			final TemporalChangeType changeType)
	{
		if (models.isEmpty())
		{
			return;
		}

		Instant now = Instant.now();
		Map<PersistenceID, HistoryModel> currentHistoriesByEntityID = new LinkedHashMap<>();
		for (HistoryModel historyModel : repository.findCurrentByEntityIDs(models.stream().map(WithID::id).toList()))
		{
			currentHistoriesByEntityID.put(historyModel.entityID(), historyModel);
		}

		Instant newValidTo =
				changeType == TemporalChangeType.DELETED ? now : TemporalValidity.defaultEndValidity();
		var historiesToSave = new ArrayList<HistoryModel>(models.size() * 2);
		for (PersistenceModel model : models)
		{
			HistoryModel currentHistory = currentHistoriesByEntityID.get(model.id());
			if (changeType == TemporalChangeType.UPDATED && currentHistory == null)
			{
				throw ResourceStateConflictException.withMessage(
						"Missing current history record for entity ID: " + model.id());
			}
			if (currentHistory != null)
			{
				currentHistory.setValidTo(now);
				historiesToSave.add(currentHistory);
			}

			historiesToSave.add(snapshotFactory.createSnapshot(model, changeType, now, now, newValidTo));
		}

		repository.saveAll(historiesToSave);
	}
}