package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.adapter;

import de.gupta.clean.crud.template.domain.model.builder.ModelBuilderFactory;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model.DomainPersistenceAdapterModel;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.repository.DomainPersistenceAdapterRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.service.DomainIDGenerator;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractDomainPersistenceIDManagement<DomainID, PersistenceID, T extends DomainPersistenceAdapterModel<DomainID, PersistenceID>>
		implements DomainPersistenceIDManagement<DomainID, PersistenceID>
{
	private final DomainPersistenceAdapterRepository<DomainID, PersistenceID, T> repository;
	private final ModelBuilderFactory<DomainPersistenceAdapterModel<DomainID, PersistenceID>,
			DomainPersistenceAdapterModel.Builder<DomainID, PersistenceID, T>>
			modelBuilderFactory;
	private final DomainIDGenerator<DomainID> domainIDGenerator;

	@Override
	public DomainID add(final PersistenceID persistenceID)
	{
		DomainID domainID = domainIDGenerator.generate();
		while (repository.existsByDomainID(domainID))
		{
			domainID = domainIDGenerator.generate(domainID);
		}
		associate(domainID, persistenceID);
		return domainID;
	}

	private void associate(final DomainID domainID, final PersistenceID persistenceID)
	{
		repository.save(modelBuilderFactory.builder().withDomainID(domainID).withPersistenceID(persistenceID)
										   .withValidFrom(Instant.now()).build());
	}

	@Override
	public Map<PersistenceID, DomainID> addBatch(final Collection<PersistenceID> persistenceIDs)
	{
		if (persistenceIDs.isEmpty())
		{
			return new HashMap<>();
		}

		Map<PersistenceID, DomainID> idMap = generateInitialDomainIDs(persistenceIDs);
		resolveDomainIDConflicts(idMap);
		saveModels(persistenceIDs, idMap);
		return idMap;
	}

	@Override
	public void update(final DomainID domainID, final PersistenceID persistenceID)
	{
		repository.findValidByDomainID(domainID).ifPresent(m -> m.setValidTo(Instant.now()));
		associate(domainID, persistenceID);
	}

	private Map<PersistenceID, DomainID> generateInitialDomainIDs(final Collection<PersistenceID> persistenceIDs)
	{
		Map<PersistenceID, DomainID> idMap = new ConcurrentHashMap<>();
		Set<DomainID> generatedDomainIDs = Collections.newSetFromMap(new ConcurrentHashMap<>());

		persistenceIDs.parallelStream().forEach(persistenceID ->
		{
			DomainID domainID;
			do
			{
				domainID = domainIDGenerator.generate();
			}
			while (!generatedDomainIDs.add(domainID));

			idMap.put(persistenceID, domainID);
		});

		return idMap;
	}

	private void resolveDomainIDConflicts(final Map<PersistenceID, DomainID> idMap)
	{
		Collection<DomainID> generatedDomainIDs = new HashSet<>(idMap.values());
		Collection<DomainID> existingDomainIDs = repository.existingDomainIDsFrom(generatedDomainIDs);
		if (existingDomainIDs.isEmpty())
		{
			return;
		}
		processExistingDomainIDs(idMap, existingDomainIDs, new HashSet<>(generatedDomainIDs));
	}

	private void processExistingDomainIDs(final Map<PersistenceID, DomainID> idMap,
										  final Collection<DomainID> existingDomainIDs,
										  final Set<DomainID> allGeneratedDomainIDs)
	{
		// Map to track which entries need new domain IDs
		Map<PersistenceID, DomainID> entriesToUpdate = new HashMap<>();

		// First pass: identify entries that need updating and generate new domain IDs
		for (Map.Entry<PersistenceID, DomainID> entry : new HashMap<>(idMap).entrySet())
		{
			if (existingDomainIDs.contains(entry.getValue()))
			{
				DomainID newDomainID;
				do
				{
					newDomainID = domainIDGenerator.generate(entry.getValue());
					// Keep generating until we find one that's not already in our set
				} while (!allGeneratedDomainIDs.add(newDomainID));

				entriesToUpdate.put(entry.getKey(), newDomainID);
			}
		}

		// If no entries to update, we're done
		if (entriesToUpdate.isEmpty())
		{
			return;
		}

		// Check all new domain IDs in a batch
		Collection<DomainID> newDomainIDs = entriesToUpdate.values();
		Collection<DomainID> existingNewDomainIDs = repository.existingDomainIDsFrom(newDomainIDs);

		// Second pass: update entries with non-existing domain IDs
		for (Map.Entry<PersistenceID, DomainID> entry : entriesToUpdate.entrySet())
		{
			DomainID newDomainID = entry.getValue();

			// If the new domain ID already exists, we'll handle it in the next iteration
			if (!existingNewDomainIDs.contains(newDomainID))
			{
				idMap.put(entry.getKey(), newDomainID);
			}
		}

		// If we still have entries that need updating, process them again
		if (!existingNewDomainIDs.isEmpty())
		{
			// Get the remaining persistence IDs that need new domain IDs
			Set<PersistenceID> remainingPersistenceIDs = entriesToUpdate.entrySet().stream()
																		.filter(entry -> existingNewDomainIDs.contains(
																				entry.getValue()))
																		.map(Map.Entry::getKey)
																		.collect(
																				java.util.stream.Collectors.toSet());

			if (!remainingPersistenceIDs.isEmpty())
			{
				// Create a new map for the remaining entries
				Map<PersistenceID, DomainID> remainingIdMap = new HashMap<>();

				// Generate new domain IDs for the remaining entries
				for (PersistenceID persistenceID : remainingPersistenceIDs)
				{
					DomainID newDomainID;
					do
					{
						newDomainID = domainIDGenerator.generate();
					}
					while (!allGeneratedDomainIDs.add(newDomainID));

					remainingIdMap.put(persistenceID, newDomainID);
				}

				// Check for conflicts again
				Collection<DomainID> remainingDomainIDs = remainingIdMap.values();
				Collection<DomainID> existingRemainingDomainIDs = repository.existingDomainIDsFrom(remainingDomainIDs);

				// If there are still conflicts, process them recursively
				if (!existingRemainingDomainIDs.isEmpty())
				{
					processExistingDomainIDs(remainingIdMap, existingRemainingDomainIDs, allGeneratedDomainIDs);
				}

				// Update the original map with the resolved entries
				idMap.putAll(remainingIdMap);
			}
		}
	}

	private void saveModels(final Collection<PersistenceID> persistenceIDs, final Map<PersistenceID, DomainID> idMap)
	{
		Instant validFrom = Instant.now();
		var models =
				persistenceIDs.stream()
							  .map(persistenceID ->
							  {
								  DomainID domainID = idMap.get(persistenceID);
								  return modelBuilderFactory.builder()
															.withDomainID(domainID)
															.withPersistenceID(persistenceID)
															.withValidFrom(validFrom)
															.build();
							  })
							  .toList();

		repository.saveAll(models);
	}

	protected AbstractDomainPersistenceIDManagement(
			final DomainPersistenceAdapterRepository<DomainID, PersistenceID, T> repository,
			final ModelBuilderFactory<DomainPersistenceAdapterModel<DomainID, PersistenceID>,
					DomainPersistenceAdapterModel.Builder<DomainID, PersistenceID, T>> modelBuilderFactory,
			final DomainIDGenerator<DomainID> domainIDGenerator)
	{
		this.repository = repository;
		this.modelBuilderFactory = modelBuilderFactory;
		this.domainIDGenerator = domainIDGenerator;
	}
}