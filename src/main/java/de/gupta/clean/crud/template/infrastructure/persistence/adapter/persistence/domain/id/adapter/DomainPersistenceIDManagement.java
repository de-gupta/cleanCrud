package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.adapter;

import java.util.Collection;
import java.util.Map;

public interface DomainPersistenceIDManagement<DomainID, PersistenceID>
{
	DomainID add(PersistenceID persistenceID);

	Map<PersistenceID, DomainID> addBatch(Collection<PersistenceID> persistenceIDs);

	void update(DomainID domainID, PersistenceID persistenceID);
}