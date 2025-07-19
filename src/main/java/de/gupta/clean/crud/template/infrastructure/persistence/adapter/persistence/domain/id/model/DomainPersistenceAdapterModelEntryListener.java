package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

public final class DomainPersistenceAdapterModelEntryListener<DomainID, PersistenceID>
{
	@PrePersist
	public void prePersist(final DomainPersistenceAdapterModel<DomainID, PersistenceID> model)
	{
		model.setTransactionTime(Instant.now());
	}

	@PreUpdate
	public void preUpdate(final DomainPersistenceAdapterModel<DomainID, PersistenceID> model)
	{
		model.setTransactionTime(Instant.now());
	}
}