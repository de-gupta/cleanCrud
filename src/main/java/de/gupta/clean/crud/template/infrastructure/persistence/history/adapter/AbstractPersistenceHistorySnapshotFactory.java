package de.gupta.clean.crud.template.infrastructure.persistence.history.adapter;

import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalChangeType;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;

import java.time.Instant;

public abstract class AbstractPersistenceHistorySnapshotFactory<PersistenceID,
		PersistenceModel extends WithID<PersistenceID>,
		HistoryModel extends de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel<PersistenceID>>
		implements TriTemporalHistorySnapshotFactory<PersistenceID, PersistenceModel, HistoryModel>
{
	@Override
	public final HistoryModel createSnapshot(
			final PersistenceModel model,
			final TemporalChangeType changeType,
			final Instant decisionTime,
			final Instant validFrom,
			final Instant validTo)
	{
		return snapshotOf(model, changeType, decisionTime, validFrom, validTo);
	}

	protected abstract HistoryModel snapshotOf(
			PersistenceModel model,
			TemporalChangeType changeType,
			Instant decisionTime,
			Instant validFrom,
			Instant validTo);
}
