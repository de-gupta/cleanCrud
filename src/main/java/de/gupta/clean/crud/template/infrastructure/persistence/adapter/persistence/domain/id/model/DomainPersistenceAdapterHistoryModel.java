package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model;

import de.gupta.clean.crud.template.domain.model.builder.ModelBuilder;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalChangeType;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TriTemporalHistoryModel;

import java.time.Instant;

public interface DomainPersistenceAdapterHistoryModel<DomainID, PersistenceID>
		extends TriTemporalHistoryModel<DomainID>
{
	PersistenceID persistenceID();

	interface Builder<DomainID, PersistenceID, T extends DomainPersistenceAdapterHistoryModel<DomainID, PersistenceID>>
			extends ModelBuilder<T>
	{
		Builder<DomainID, PersistenceID, T> withDomainID(DomainID domainID);

		Builder<DomainID, PersistenceID, T> withPersistenceID(PersistenceID persistenceID);

		Builder<DomainID, PersistenceID, T> withChangeType(TemporalChangeType changeType);

		Builder<DomainID, PersistenceID, T> withDecisionTime(Instant decisionTime);

		Builder<DomainID, PersistenceID, T> withValidFrom(Instant validFrom);

		Builder<DomainID, PersistenceID, T> withValidTo(Instant validTo);
	}
}