package de.gupta.clean.crud.template.infrastructure.persistence.history.adapter;

import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalChangeType;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;

import java.time.Instant;

public interface TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel extends WithID<PersistenceID>,
		HistoryModel extends TriTemporalHistoryModel<PersistenceID>>
{
	HistoryModel createSnapshot(
			PersistenceModel model,
			TemporalChangeType changeType,
			Instant decisionTime,
			Instant validFrom,
			Instant validTo);
}