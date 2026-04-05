package de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.adapter;

import de.gupta.clean.crud.template.domain.model.builder.ModelBuilderFactory;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.UnexpectedResourceException;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model.DomainPersistenceAdapterHistoryModel;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.model.DomainPersistenceAdapterModel;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.repository.DomainPersistenceAdapterRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.adapter.persistence.domain.id.service.DomainIDGenerator;
import de.gupta.clean.crud.template.infrastructure.persistence.history.audit.AuditActor;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalChangeType;
import de.gupta.clean.crud.template.infrastructure.persistence.history.model.TemporalValidity;
import de.gupta.clean.crud.template.infrastructure.persistence.history.repository.TriTemporalHistoryRepository;
import de.gupta.clean.crud.template.infrastructure.persistence.history.service.AuditActorSupplier;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractDomainPersistenceIDManagement<DomainID, PersistenceID,
		T extends DomainPersistenceAdapterModel<DomainID, PersistenceID>,
		H extends DomainPersistenceAdapterHistoryModel<DomainID, PersistenceID>>
		implements DomainPersistenceIDManagement<DomainID, PersistenceID>
{
	private final DomainPersistenceAdapterRepository<DomainID, PersistenceID, T> repository;
	private final ModelBuilderFactory<DomainPersistenceAdapterModel<DomainID, PersistenceID>,
			? extends DomainPersistenceAdapterModel.Builder<DomainID, PersistenceID, T>>
			modelBuilderFactory;
	private final TriTemporalHistoryRepository<DomainID, H> historyRepository;
	private final ModelBuilderFactory<DomainPersistenceAdapterHistoryModel<DomainID, PersistenceID>,
			? extends DomainPersistenceAdapterHistoryModel.Builder<DomainID, PersistenceID, H>>
			historyModelBuilderFactory;
	private final DomainIDGenerator<DomainID> domainIDGenerator;
	private final AuditActorSupplier auditActorSupplier;

	@Override
	public DomainID add(final PersistenceID persistenceID)
	{
		DomainID domainID = domainIDGenerator.generate();
		while (repository.existsByDomainID(domainID))
		{
			domainID = domainIDGenerator.generate(domainID);
		}
		associate(domainID, persistenceID, auditActorSupplier.get());
		return domainID;
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
		saveModels(persistenceIDs, idMap, auditActorSupplier.get());
		return idMap;
	}

	@Override
	public void update(final DomainID domainID, final PersistenceID persistenceID)
	{
		Instant decisionTime = Instant.now();
		AuditActor auditActor = auditActorSupplier.get();
		repository.findByDomainID(domainID).ifPresentOrElse(currentMapping ->
		{
			closeCurrentHistory(domainID, decisionTime);
			currentMapping.setPersistenceID(persistenceID);
			repository.save(currentMapping);
			recordHistory(domainID, persistenceID, TemporalChangeType.UPDATED, decisionTime,
					TemporalValidity.defaultEndValidity(), auditActor);
		}, () -> handleMissingCurrentMappingOnUpdate(domainID, persistenceID, decisionTime, auditActor));
	}

	@Override
	public void delete(final DomainID domainID)
	{
		Instant decisionTime = Instant.now();
		AuditActor auditActor = auditActorSupplier.get();
		T currentMapping = repository.findByDomainID(domainID)
		                             .orElseThrow(() -> UnexpectedResourceException.withMessage(
											 "Missing current mapping for domain ID: " + domainID));
		closeCurrentHistory(domainID, decisionTime);
		recordHistory(domainID, currentMapping.persistenceID(), TemporalChangeType.DELETED, decisionTime,
				decisionTime, auditActor);
		repository.delete(currentMapping);
	}

	@Override
	public void deleteAll(final Collection<DomainID> domainIDs)
	{
		if (domainIDs.isEmpty())
		{
			return;
		}

		Instant decisionTime = Instant.now();
		AuditActor auditActor = auditActorSupplier.get();
		Map<DomainID, T> mappingsByDomainID = repository.findByDomainIDs(domainIDs)
		                                                .stream()
		                                                .collect(java.util.stream.Collectors.toMap(
																DomainPersistenceAdapterModel::domainID,
																java.util.function.Function.identity()));
		for (DomainID domainID : domainIDs)
		{
			if (!mappingsByDomainID.containsKey(domainID))
			{
				throw UnexpectedResourceException.withMessage("Missing current mapping for domain ID: " + domainID);
			}
		}

		for (DomainID domainID : domainIDs)
		{
			closeCurrentHistory(domainID, decisionTime);
		}

		historyRepository.saveAll(domainIDs.stream()
		                                   .map(mappingsByDomainID::get)
		                                   .map(mapping -> buildHistory(
												   mapping.domainID(),
												   mapping.persistenceID(),
												   TemporalChangeType.DELETED,
												   decisionTime,
												   decisionTime,
												   auditActor))
		                                   .toList());
		repository.deleteAll(new ArrayList<>(mappingsByDomainID.values()));
	}

	protected void handleMissingCurrentMappingOnUpdate(
			final DomainID domainID,
			final PersistenceID persistenceID,
			final Instant decisionTime,
			final AuditActor auditActor)
	{
		repository.save(modelBuilderFactory.builder().withDomainID(domainID).withPersistenceID(persistenceID).build());
		recordHistory(domainID, persistenceID, TemporalChangeType.CREATED, decisionTime,
				TemporalValidity.defaultEndValidity(), auditActor);
	}

	private void associate(final DomainID domainID, final PersistenceID persistenceID, final AuditActor auditActor)
	{
		Instant decisionTime = Instant.now();
		repository.save(modelBuilderFactory.builder().withDomainID(domainID).withPersistenceID(persistenceID).build());
		recordHistory(domainID, persistenceID, TemporalChangeType.CREATED, decisionTime,
				TemporalValidity.defaultEndValidity(), auditActor);
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

	private void saveModels(
			final Collection<PersistenceID> persistenceIDs,
			final Map<PersistenceID, DomainID> idMap,
			final AuditActor auditActor)
	{
		Instant decisionTime = Instant.now();
		var models =
				persistenceIDs.stream()
				              .map(persistenceID ->
							  {
								  DomainID domainID = idMap.get(persistenceID);
								  return modelBuilderFactory.builder()
					                                        .withDomainID(domainID)
					                                        .withPersistenceID(persistenceID)
					                                        .build();
							  })
				              .toList();

		repository.saveAll(models);
		historyRepository.saveAll(persistenceIDs.stream()
		                                        .map(persistenceID ->
												{
													DomainID domainID = idMap.get(persistenceID);
													return buildHistory(
															domainID,
															persistenceID,
															TemporalChangeType.CREATED,
															decisionTime,
															TemporalValidity.defaultEndValidity(),
															auditActor);
												})
		                                        .toList());
	}

	private void closeCurrentHistory(final DomainID domainID, final Instant validTo)
	{
		historyRepository.findCurrentByEntityID(domainID).ifPresent(history ->
		{
			history.setValidTo(validTo);
			historyRepository.save(history);
		});
	}

	private void recordHistory(
			final DomainID domainID,
			final PersistenceID persistenceID,
			final TemporalChangeType changeType,
			final Instant decisionTime,
			final Instant validTo,
			final AuditActor auditActor)
	{
		historyRepository.save(buildHistory(domainID, persistenceID, changeType, decisionTime, validTo, auditActor));
	}

	private H buildHistory(
			final DomainID domainID,
			final PersistenceID persistenceID,
			final TemporalChangeType changeType,
			final Instant decisionTime,
			final Instant validTo,
			final AuditActor auditActor)
	{
		H historyModel = historyModelBuilderFactory.builder()
		                                           .withDomainID(domainID)
		                                           .withPersistenceID(persistenceID)
		                                           .withChangeType(changeType)
		                                           .withDecisionTime(decisionTime)
		                                           .withValidFrom(decisionTime)
		                                           .withValidTo(validTo)
		                                           .build();
		historyModel.setAuditActor(auditActor);
		return historyModel;
	}

	protected AbstractDomainPersistenceIDManagement(
			final DomainPersistenceAdapterRepository<DomainID, PersistenceID, T> repository,
			final ModelBuilderFactory<DomainPersistenceAdapterModel<DomainID, PersistenceID>,
					? extends DomainPersistenceAdapterModel.Builder<DomainID, PersistenceID, T>> modelBuilderFactory,
			final TriTemporalHistoryRepository<DomainID, H> historyRepository,
			final ModelBuilderFactory<DomainPersistenceAdapterHistoryModel<DomainID, PersistenceID>,
					? extends DomainPersistenceAdapterHistoryModel.Builder<DomainID, PersistenceID, H>> historyModelBuilderFactory,
			final DomainIDGenerator<DomainID> domainIDGenerator,
			final AuditActorSupplier auditActorSupplier)
	{
		this.repository = repository;
		this.modelBuilderFactory = modelBuilderFactory;
		this.historyRepository = historyRepository;
		this.historyModelBuilderFactory = historyModelBuilderFactory;
		this.domainIDGenerator = domainIDGenerator;
		this.auditActorSupplier = auditActorSupplier;
	}
}