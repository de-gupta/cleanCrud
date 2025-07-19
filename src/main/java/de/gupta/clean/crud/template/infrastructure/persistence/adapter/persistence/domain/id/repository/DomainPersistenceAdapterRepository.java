package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.repository;

import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model.DomainPersistenceAdapterModel;

import java.util.Collection;
import java.util.Optional;

public interface DomainPersistenceAdapterRepository<DomainID, PersistenceID, Model extends DomainPersistenceAdapterModel<DomainID, PersistenceID>>
{
	boolean existsByDomainID(DomainID domainID);

	Collection<DomainID> existingDomainIDsFrom(Collection<DomainID> domainIDs);

	Model save(Model model);

	void saveAll(Collection<Model> models);

	Optional<Model> findValidByDomainID(DomainID domainID);

	Optional<Model> findValidByPersistenceID(PersistenceID persistenceID);
}