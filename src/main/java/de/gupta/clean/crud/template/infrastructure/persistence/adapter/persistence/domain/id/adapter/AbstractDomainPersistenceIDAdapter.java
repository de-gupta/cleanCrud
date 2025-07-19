package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.adapter;

import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model.DomainPersistenceAdapterModel;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.repository.DomainPersistenceAdapterRepository;

import java.util.Optional;

public abstract class AbstractDomainPersistenceIDAdapter<DomainID, PersistenceID>
		implements DomainPersistenceIDAdapter<DomainID, PersistenceID>
{
	private final DomainPersistenceAdapterRepository<DomainID, PersistenceID, ?> repository;

	@Override
	public Optional<PersistenceID> toPersistenceID(final DomainID domainID)
	{
		return repository.findValidByDomainID(domainID).map(DomainPersistenceAdapterModel::persistenceID);
	}

	@Override
	public Optional<DomainID> toDomainID(final PersistenceID persistenceID)
	{
		return repository.findValidByPersistenceID(persistenceID).map(DomainPersistenceAdapterModel::domainID);
	}

	protected AbstractDomainPersistenceIDAdapter(
			final DomainPersistenceAdapterRepository<DomainID, PersistenceID, ?> repository)
	{
		this.repository = repository;
	}
}