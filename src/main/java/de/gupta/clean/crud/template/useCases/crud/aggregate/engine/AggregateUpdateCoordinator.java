package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.SatellitePersistenceOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

final class AggregateUpdateCoordinator
{
	private final SatelliteRelationshipPlanner relationshipPlanner;
	private final SatelliteReferenceResolver referenceResolver;

	<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void putAtId(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainId masterDomainId,
			final MasterDomainModelCreate masterDomainModelCreate)
	{
		var current = definition.fetchPort().findById(masterDomainId);
		if (current.isEmpty())
		{
			putNewAtId(definition, relationships, masterDomainId, masterDomainModelCreate);
			return;
		}

		MasterDomainModel replacementMasterDomainModel = definition.createBuilder().toModel(masterDomainModelCreate);
		for (var relationship : relationships)
		{
			replacementMasterDomainModel = applyPutRelationship(
					relationship,
					current.get().model(),
					replacementMasterDomainModel,
					masterDomainModelCreate);
		}
		validatePatch(definition, current.get().model(), replacementMasterDomainModel);
		definition.mutationPort().put(masterDomainId, replacementMasterDomainModel);
	}

	<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> IdentifiedModel<MasterDomainId, MasterDomainModel> updateById(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainId masterDomainId,
			final MasterDomainModelUpdatePatch masterDomainModelUpdatePatch)
	{
		var current = definition.fetchPort().findById(masterDomainId)
		                        .orElseThrow(() -> ResourceNotFoundException.withId(masterDomainId));
		MasterDomainModel updatedMasterDomainModel =
				definition.patcher().patchModel(current.model(), masterDomainModelUpdatePatch);
		for (var relationship : relationships)
		{
			updatedMasterDomainModel = applyUpdateRelationship(
					relationship,
					current.model(),
					updatedMasterDomainModel,
					masterDomainModelUpdatePatch);
		}
		validatePatch(definition, current.model(), updatedMasterDomainModel);
		return definition.mutationPort().update(masterDomainId, updatedMasterDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void putNewAtId(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainId masterDomainId,
			final MasterDomainModelCreate masterDomainModelCreate)
	{
		MasterDomainModel masterDomainModel = definition.createBuilder().toModel(masterDomainModelCreate);
		for (var relationship : relationships)
		{
			if (relationship.linkStrategy().persistenceOrder() == SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER)
			{
				masterDomainModel =
						applyPutCreateRelationship(relationship, masterDomainModel, masterDomainModelCreate);
			}
		}
		validateInsertion(definition, masterDomainModel);
		definition.mutationPort().put(masterDomainId, masterDomainModel);

		MasterDomainModel linkedMasterDomainModel = masterDomainModel;
		for (var relationship : relationships)
		{
			if (relationship.linkStrategy().persistenceOrder() != SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER)
			{
				linkedMasterDomainModel =
						applyPutCreateRelationship(relationship, linkedMasterDomainModel, masterDomainModelCreate);
			}
		}
		if (linkedMasterDomainModel != masterDomainModel)
		{
			definition.mutationPort().put(masterDomainId, linkedMasterDomainModel);
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	MasterDomainModel applyPutCreateRelationship(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModel masterDomainModel,
			final MasterDomainModelCreate masterDomainModelCreate)
	{
		return relationshipPlanner.replaceLinkedSatelliteDomainIds(
				relationship,
				masterDomainModel,
				resolveSatelliteDomainIdsForCreate(relationship, masterDomainModelCreate));
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	MasterDomainModel applyPutRelationship(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModel currentMasterDomainModel,
			final MasterDomainModel replacementMasterDomainModel,
			final MasterDomainModelCreate masterDomainModelCreate)
	{
		var targetSatelliteDomainIds = resolveSatelliteDomainIdsForCreate(relationship, masterDomainModelCreate);
		var currentSatelliteDomainIds =
				relationshipPlanner.currentLinkedSatelliteDomainIds(relationship, currentMasterDomainModel);
		deleteOrphanedSatellitesIfNeeded(
				relationship,
				difference(currentSatelliteDomainIds, targetSatelliteDomainIds));
		return relationshipPlanner.replaceLinkedSatelliteDomainIds(
				relationship,
				replacementMasterDomainModel,
				targetSatelliteDomainIds);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	MasterDomainModel applyUpdateRelationship(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModel currentMasterDomainModel,
			final MasterDomainModel updatedMasterDomainModel,
			final MasterDomainModelUpdatePatch masterDomainModelUpdatePatch)
	{
		var mutationIntents = relationshipPlanner.mutationIntents(relationship, masterDomainModelUpdatePatch);
		if (mutationIntents.isEmpty())
		{
			return updatedMasterDomainModel;
		}
		if (!relationship.lifecycleSemantics().cascadeUpdate())
		{
			throw AggregateRelationshipExecutionNotSupportedException.withMessage(
					"Relationship '" + relationship.name() + "' does not allow satellite update participation");
		}

		return switch (relationship.reconciliationStrategy())
		{
			case REPLACE -> applyReplaceUpdateRelationship(
					relationship,
					currentMasterDomainModel,
					updatedMasterDomainModel,
					mutationIntents);
			case MERGE_BY_ID -> applyMergeByIdUpdateRelationship(
					relationship,
					currentMasterDomainModel,
					updatedMasterDomainModel,
					mutationIntents);
		};
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	MasterDomainModel applyReplaceUpdateRelationship(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModel currentMasterDomainModel,
			final MasterDomainModel updatedMasterDomainModel,
			final List<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>>
					mutationIntents)
	{
		var currentSatelliteDomainIds =
				relationshipPlanner.currentLinkedSatelliteDomainIds(relationship, currentMasterDomainModel);
		var targetSatelliteDomainIds = new ArrayList<SatelliteDomainId>();
		for (var mutationIntent : mutationIntents)
		{
			switch (mutationIntent)
			{
				case SatelliteMutationIntent.ReferenceSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> referenceIntent ->
				{
					referenceResolver.requiredSatellite(relationship, referenceIntent.satelliteDomainId());
					targetSatelliteDomainIds.add(referenceIntent.satelliteDomainId());
				}
				case SatelliteMutationIntent.CreateSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> createIntent -> targetSatelliteDomainIds.add(
						createSatellite(relationship, createIntent.satelliteDomainModelCreate()));
				case SatelliteMutationIntent.UpdateSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> updateIntent ->
				{
					requireCurrentlyLinkedSatelliteDomainId(
							relationship,
							currentSatelliteDomainIds,
							updateIntent.satelliteDomainId(),
							"update");
					updateSatellite(relationship, updateIntent.satelliteDomainId(),
							updateIntent.satelliteDomainModelUpdatePatch());
					targetSatelliteDomainIds.add(updateIntent.satelliteDomainId());
				}
				case SatelliteMutationIntent.UpsertCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> upsertCurrentIntent ->
				{
					if (currentSatelliteDomainIds.isEmpty())
					{
						targetSatelliteDomainIds.add(createSatellite(
								relationship,
								upsertCurrentIntent.satelliteDomainModelCreate()));
						break;
					}
					var currentSatelliteDomainId =
							requiredCurrentLinkedSatelliteDomainId(relationship, currentSatelliteDomainIds, "update");
					updateSatellite(relationship, currentSatelliteDomainId,
							upsertCurrentIntent.satelliteDomainModelUpdatePatch());
					targetSatelliteDomainIds.add(currentSatelliteDomainId);
				}
				case SatelliteMutationIntent.UpdateCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> updateCurrentIntent ->
				{
					var currentSatelliteDomainId =
							requiredCurrentLinkedSatelliteDomainId(relationship, currentSatelliteDomainIds, "update");
					updateSatellite(relationship, currentSatelliteDomainId,
							updateCurrentIntent.satelliteDomainModelUpdatePatch());
					targetSatelliteDomainIds.add(currentSatelliteDomainId);
				}
				case SatelliteMutationIntent.RemoveSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> ignored ->
				{
				}
				case SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> ignored ->
				{
				}
			}
		}
		deleteOrphanedSatellitesIfNeeded(
				relationship,
				difference(currentSatelliteDomainIds, targetSatelliteDomainIds));
		return relationshipPlanner.replaceLinkedSatelliteDomainIds(
				relationship,
				updatedMasterDomainModel,
				targetSatelliteDomainIds);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	MasterDomainModel applyMergeByIdUpdateRelationship(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModel currentMasterDomainModel,
			final MasterDomainModel updatedMasterDomainModel,
			final List<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>>
					mutationIntents)
	{
		var currentSatelliteDomainIds =
				relationshipPlanner.currentLinkedSatelliteDomainIds(relationship, currentMasterDomainModel);
		var targetSatelliteDomainIds = new LinkedHashSet<>(currentSatelliteDomainIds);
		var removedSatelliteDomainIds = new LinkedHashSet<SatelliteDomainId>();
		for (var mutationIntent : mutationIntents)
		{
			switch (mutationIntent)
			{
				case SatelliteMutationIntent.ReferenceSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> referenceIntent ->
				{
					referenceResolver.requiredSatellite(relationship, referenceIntent.satelliteDomainId());
					targetSatelliteDomainIds.add(referenceIntent.satelliteDomainId());
				}
				case SatelliteMutationIntent.CreateSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> createIntent -> targetSatelliteDomainIds.add(
						createSatellite(relationship, createIntent.satelliteDomainModelCreate()));
				case SatelliteMutationIntent.UpdateSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> updateIntent ->
				{
					requireCurrentlyLinkedSatelliteDomainId(
							relationship,
							currentSatelliteDomainIds,
							updateIntent.satelliteDomainId(),
							"update");
					updateSatellite(relationship, updateIntent.satelliteDomainId(),
							updateIntent.satelliteDomainModelUpdatePatch());
					targetSatelliteDomainIds.add(updateIntent.satelliteDomainId());
				}
				case SatelliteMutationIntent.UpsertCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> upsertCurrentIntent ->
				{
					if (currentSatelliteDomainIds.isEmpty())
					{
						targetSatelliteDomainIds.add(createSatellite(
								relationship,
								upsertCurrentIntent.satelliteDomainModelCreate()));
						break;
					}
					var currentSatelliteDomainId =
							requiredCurrentLinkedSatelliteDomainId(relationship, currentSatelliteDomainIds, "update");
					updateSatellite(relationship, currentSatelliteDomainId,
							upsertCurrentIntent.satelliteDomainModelUpdatePatch());
					targetSatelliteDomainIds.add(currentSatelliteDomainId);
				}
				case SatelliteMutationIntent.UpdateCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> updateCurrentIntent ->
				{
					var currentSatelliteDomainId =
							requiredCurrentLinkedSatelliteDomainId(relationship, currentSatelliteDomainIds, "update");
					updateSatellite(relationship, currentSatelliteDomainId,
							updateCurrentIntent.satelliteDomainModelUpdatePatch());
					targetSatelliteDomainIds.add(currentSatelliteDomainId);
				}
				case SatelliteMutationIntent.RemoveSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> removeIntent ->
				{
					requireCurrentlyLinkedSatelliteDomainId(
							relationship,
							currentSatelliteDomainIds,
							removeIntent.satelliteDomainId(),
							"remove");
					targetSatelliteDomainIds.remove(removeIntent.satelliteDomainId());
					removedSatelliteDomainIds.add(removeIntent.satelliteDomainId());
				}
				case SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> ignored ->
				{
					var currentSatelliteDomainId =
							requiredCurrentLinkedSatelliteDomainId(relationship, currentSatelliteDomainIds, "remove");
					targetSatelliteDomainIds.remove(currentSatelliteDomainId);
					removedSatelliteDomainIds.add(currentSatelliteDomainId);
				}
			}
		}
		deleteOrphanedSatellitesIfNeeded(relationship, removedSatelliteDomainIds);
		return relationshipPlanner.replaceLinkedSatelliteDomainIds(
				relationship,
				updatedMasterDomainModel,
				new ArrayList<>(targetSatelliteDomainIds));
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	List<SatelliteDomainId> resolveSatelliteDomainIdsForCreate(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModelCreate masterDomainModelCreate)
	{
		var satelliteDomainIds = new ArrayList<SatelliteDomainId>();
		for (var createIntent : relationshipPlanner.createIntents(relationship, masterDomainModelCreate))
		{
			switch (createIntent)
			{
				case SatelliteCreateIntent.ReferenceSatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate> referenceIntent ->
				{
					referenceResolver.requiredSatellite(relationship, referenceIntent.satelliteDomainId());
					satelliteDomainIds.add(referenceIntent.satelliteDomainId());
				}
				case SatelliteCreateIntent.InlineSatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate> inlineIntent ->
				{
					if (!relationship.lifecycleSemantics().cascadeCreate())
					{
						throw AggregateRelationshipExecutionNotSupportedException.withMessage(
								"Relationship '" + relationship.name() + "' does not allow satellite create participation");
					}
					satelliteDomainIds.add(createSatellite(relationship, inlineIntent.satelliteDomainModelCreate()));
				}
				case SatelliteCreateIntent.NoSatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate> ignored ->
				{
				}
			}
		}
		return satelliteDomainIds;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId createSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainModelCreate satelliteDomainModelCreate)
	{
		var satelliteDomainModel =
				relationship.satelliteDefinition().createBuilder().toModel(satelliteDomainModelCreate);
		validateSatelliteInsertion(relationship, satelliteDomainModel);
		return relationship.satelliteDefinition().mutationPort().create(satelliteDomainModel).id();
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void updateSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainId satelliteDomainId,
			final SatelliteDomainModelUpdatePatch satelliteDomainModelUpdatePatch)
	{
		var currentSatellite = referenceResolver.requiredSatellite(relationship, satelliteDomainId);
		var updatedSatelliteDomainModel =
				relationship.satelliteDefinition().patcher()
				            .patchModel(currentSatellite.model(), satelliteDomainModelUpdatePatch);
		validateSatellitePatch(relationship, currentSatellite.model(), updatedSatelliteDomainModel);
		relationship.satelliteDefinition().mutationPort().update(satelliteDomainId, updatedSatelliteDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void deleteOrphanedSatellitesIfNeeded(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> removedSatelliteDomainIds)
	{
		if (!relationship.lifecycleSemantics().orphanDelete())
		{
			return;
		}
		for (var satelliteDomainId : removedSatelliteDomainIds)
		{
			var currentSatellite = referenceResolver.requiredSatellite(relationship, satelliteDomainId);
			relationship.satelliteDefinition().deletionPolicy().validateDeletion(currentSatellite.model());
			relationship.satelliteDefinition().mutationPort().delete(satelliteDomainId);
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validateInsertion(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainModel masterDomainModel)
	{
		if (!definition.securityPolicy().isAccessAllowed(masterDomainModel))
		{
			throw AccessDeniedException.withMessage("Access not allowed");
		}
		definition.insertionPolicy().validateInsertion(masterDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validatePatch(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainModel originalMasterDomainModel,
			final MasterDomainModel replacementMasterDomainModel)
	{
		if (!definition.securityPolicy().isAccessAllowed(originalMasterDomainModel)
				|| !definition.securityPolicy().isAccessAllowed(replacementMasterDomainModel))
		{
			throw AccessDeniedException.withMessage("Access not allowed");
		}
		definition.patchPolicy().validatePatchAttempt(originalMasterDomainModel, replacementMasterDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void validateSatelliteInsertion(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainModel satelliteDomainModel)
	{
		if (!relationship.satelliteDefinition().securityPolicy().isAccessAllowed(satelliteDomainModel))
		{
			throw AccessDeniedException.withMessage("Access not allowed");
		}
		relationship.satelliteDefinition().insertionPolicy().validateInsertion(satelliteDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void validateSatellitePatch(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainModel originalSatelliteDomainModel,
			final SatelliteDomainModel replacementSatelliteDomainModel)
	{
		if (!relationship.satelliteDefinition().securityPolicy().isAccessAllowed(originalSatelliteDomainModel)
				|| !relationship.satelliteDefinition().securityPolicy()
				                .isAccessAllowed(replacementSatelliteDomainModel))
		{
			throw AccessDeniedException.withMessage("Access not allowed");
		}
		relationship.satelliteDefinition().patchPolicy()
		            .validatePatchAttempt(originalSatelliteDomainModel, replacementSatelliteDomainModel);
	}

	private <DomainId> List<DomainId> difference(
			final Collection<DomainId> left,
			final Collection<DomainId> right)
	{
		var difference = new ArrayList<>(left);
		difference.removeAll(right);
		return difference;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void requireCurrentlyLinkedSatelliteDomainId(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> currentSatelliteDomainIds,
			final SatelliteDomainId satelliteDomainId,
			final String operation)
	{
		if (!currentSatelliteDomainIds.contains(satelliteDomainId))
		{
			throw InvalidRequestException.withMessage(
					"Relationship '" + relationship.name() + "' cannot " + operation
							+ " a satellite that is not currently linked");
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId requiredCurrentLinkedSatelliteDomainId(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> currentSatelliteDomainIds,
			final String operation)
	{
		if (currentSatelliteDomainIds.isEmpty())
		{
			throw InvalidRequestException.withMessage(
					"Relationship '" + relationship.name() + "' cannot " + operation
							+ " because no satellite is currently linked");
		}
		if (currentSatelliteDomainIds.size() > 1)
		{
			throw InvalidRequestException.withMessage(
					"Relationship '" + relationship.name() + "' cannot " + operation
							+ " implicitly because more than one satellite is currently linked");
		}
		return currentSatelliteDomainIds.iterator().next();
	}

	AggregateUpdateCoordinator(
			final SatelliteRelationshipPlanner relationshipPlanner,
			final SatelliteReferenceResolver referenceResolver)
	{
		this.relationshipPlanner = relationshipPlanner;
		this.referenceResolver = referenceResolver;
	}
}
