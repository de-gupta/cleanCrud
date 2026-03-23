package de.gupta.clean.crud.template.infrastructure.persistence.history.model;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

public final class TriTemporalHistoryModelEntryListener<EntityID>
{
	@PrePersist
	public void prePersist(final TriTemporalHistoryModel<EntityID> model)
	{
		model.setTransactionTime(Instant.now());
	}

	@PreUpdate
	public void preUpdate(final TriTemporalHistoryModel<EntityID> model)
	{
		model.setTransactionTime(Instant.now());
	}
}