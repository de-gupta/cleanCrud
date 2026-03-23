package de.gupta.clean.crud.template.infrastructure.persistence.history.repository;

import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;

import java.util.Collection;
import java.util.Optional;

public interface TriTemporalHistoryRepository<EntityID, HistoryModel extends TriTemporalHistoryModel<EntityID>>
{
	HistoryModel save(HistoryModel model);

	void saveAll(Collection<HistoryModel> models);

	Optional<HistoryModel> findCurrentByEntityID(EntityID entityID);

	Collection<HistoryModel> findCurrentByEntityIDs(Collection<EntityID> entityIDs);
}