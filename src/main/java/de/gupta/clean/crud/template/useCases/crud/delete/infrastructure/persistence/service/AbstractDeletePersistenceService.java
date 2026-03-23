package de.gupta.clean.crud.template.useCases.crud.delete.infrastructure.persistence.service;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.adapter.DomainPersistenceIDAdapter;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.DeletePersistenceService;
import de.gupta.clean.crud.template.useCases.crud.fetch.infrastructure.persistence.service.FetchPersistenceModelRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

public abstract class AbstractDeletePersistenceService<DomainID, PersistenceID,
		PersistenceModel extends WithID<PersistenceID>>
		implements DeletePersistenceService<DomainID>
{
	private final FetchPersistenceModelRepository<PersistenceModel, PersistenceID> fetchRepository;
	private final DeletePersistenceModelRepository<PersistenceID> deleteRepository;
	private final DomainPersistenceIDAdapter<DomainID, PersistenceID> idAdapter;

	@Override
	public void deleteById(final DomainID id)
	{
		final var persistenceID = idAdapter.toPersistenceID(id)
										   .orElseThrow(() -> ResourceNotFoundException.withId(id));
		deleteRepository.deleteById(persistenceID);
	}

	@Override
	public void deleteAllById(final Collection<DomainID> ids)
	{
		var domainIDs = new ArrayList<>(ids);
		var persistenceIDs = new ArrayList<PersistenceID>(domainIDs.size());
		for (DomainID id : domainIDs)
		{
			persistenceIDs.add(idAdapter.toPersistenceID(id)
										.orElseThrow(() -> ResourceNotFoundException.withId(id)));
		}

		var existingPersistenceIDs = fetchRepository.findByIds(persistenceIDs)
													.stream()
													.map(PersistenceModel::id)
													.collect(java.util.stream.Collectors.toCollection(
															LinkedHashSet::new));
		for (int i = 0; i < persistenceIDs.size(); i++)
		{
			if (!existingPersistenceIDs.contains(persistenceIDs.get(i)))
			{
				throw ResourceNotFoundException.withId(domainIDs.get(i));
			}
		}

		deleteRepository.deleteAllById(persistenceIDs);
	}

	protected AbstractDeletePersistenceService(
			final FetchPersistenceModelRepository<PersistenceModel, PersistenceID> fetchRepository,
			final DeletePersistenceModelRepository<PersistenceID> deleteRepository,
			final DomainPersistenceIDAdapter<DomainID, PersistenceID> idAdapter)
	{
		this.fetchRepository = fetchRepository;
		this.deleteRepository = deleteRepository;
		this.idAdapter = idAdapter;
	}
}