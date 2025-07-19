package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.adapter;

import java.util.Optional;

public interface DomainPersistenceIDAdapter<DomainID, PersistenceID>
{
	Optional<PersistenceID> toPersistenceID(DomainID domainID);

	Optional<DomainID> toDomainID(PersistenceID persistenceID);
}