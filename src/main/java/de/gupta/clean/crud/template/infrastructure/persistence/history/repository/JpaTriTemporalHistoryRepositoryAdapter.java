package de.gupta.clean.crud.template.infrastructure.persistence.history.repository;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceStateConflictException;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;

import java.time.Instant;
import java.util.*;

public class JpaTriTemporalHistoryRepositoryAdapter<EntityID, HistoryModel extends TriTemporalHistoryModel<EntityID>>
		implements TriTemporalHistoryRepository<EntityID, HistoryModel>
{
	private final TriTemporalHistoryJpaRepository<EntityID, HistoryModel> repository;

	public JpaTriTemporalHistoryRepositoryAdapter(
			final TriTemporalHistoryJpaRepository<EntityID, HistoryModel> repository)
	{
		this.repository = repository;
	}

	@Override
	public HistoryModel save(final HistoryModel model)
	{
		return repository.save(model);
	}

	@Override
	public void saveAll(final Collection<HistoryModel> models)
	{
		repository.saveAll(models);
	}

	@Override
	public Optional<HistoryModel> findCurrentByEntityID(final EntityID entityID)
	{
		return findCurrentByEntityIDs(java.util.List.of(entityID)).stream().findFirst();
	}

	@Override
	public Collection<HistoryModel> findCurrentByEntityIDs(final Collection<EntityID> entityIDs)
	{
		if (entityIDs.isEmpty())
		{
			return List.of();
		}

		Instant now = Instant.now();
		Collection<HistoryModel> currentHistories =
				repository.findAllByEntityIDInAndValidFromIsBeforeAndValidToIsAfter(entityIDs, now, now);
		Map<EntityID, HistoryModel> currentHistoryByEntityID = new LinkedHashMap<>();
		Collection<EntityID> conflictingEntityIDs = new ArrayList<>();
		for (HistoryModel historyModel : currentHistories)
		{
			HistoryModel existingHistory = currentHistoryByEntityID.putIfAbsent(historyModel.entityID(), historyModel);
			if (existingHistory != null)
			{
				conflictingEntityIDs.add(historyModel.entityID());
			}
		}
		if (!conflictingEntityIDs.isEmpty())
		{
			throw ResourceStateConflictException.withMessage(
					"Multiple current history records found for entity IDs: " + conflictingEntityIDs);
		}
		return currentHistoryByEntityID.values();
	}
}