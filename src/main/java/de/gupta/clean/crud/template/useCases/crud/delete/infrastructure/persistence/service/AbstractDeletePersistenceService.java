package de.gupta.clean.crud.template.useCases.crud.delete.infrastructure.persistence.service;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.adapter.DomainPersistenceIDAdapter;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.DeletePersistenceService;

public abstract class AbstractDeletePersistenceService<DomainID, PersistenceID>
		implements DeletePersistenceService<DomainID>
{
	private final DeletePersistenceModelRepository<PersistenceID> deleteRepository;
	private final DomainPersistenceIDAdapter<DomainID, PersistenceID> idAdapter;

	@Override
	public void deleteById(final DomainID id)
	{
		final var persistenceID = idAdapter.toPersistenceID(id)
										   .orElseThrow(() -> ResourceNotFoundException.withId(id));
		deleteRepository.deleteById(persistenceID);
	}

	protected AbstractDeletePersistenceService(
			final DeletePersistenceModelRepository<PersistenceID> deleteRepository,
			final DomainPersistenceIDAdapter<DomainID, PersistenceID> idAdapter)
	{
		this.deleteRepository = deleteRepository;
		this.idAdapter = idAdapter;
	}
}