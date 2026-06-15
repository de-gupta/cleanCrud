package de.gupta.clean.crud.template.domain.service.aggregate.mechanics.coordinator;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.domain.aggregate.relationship.Cardinality;
import de.gupta.clean.crud.template.domain.aggregate.relationship.SatellitePersistenceOrder;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.intent.SatelliteCreateIntentResolver;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.relationship.AggregateRelationshipExecutionNotSupportedException;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.relationship.SatelliteReferenceResolver;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.relationship.SatelliteRelationshipPlanner;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.validator.AggregateMutationValidationSupport;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public final class AggregateUpdateCoordinator
{
	private final SatelliteRelationshipPlanner relationshipPlanner;
	private final SatelliteReferenceResolver referenceResolver;
	private final SatelliteCreateIntentResolver createIntentResolver;
	private final AggregateMutationValidationSupport validationSupport;

	public static AggregateUpdateCoordinator with(
			final SatelliteRelationshipPlanner relationshipPlanner,
			final SatelliteReferenceResolver referenceResolver,
			final SatelliteCreateIntentResolver createIntentResolver,
			final AggregateMutationValidationSupport validationSupport)
	{
		return new AggregateUpdateCoordinator(relationshipPlanner, referenceResolver, createIntentResolver,
				validationSupport);
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void putAtId(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
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

		var replacementMasterDomainModel = replaceLinkedSatellitesForPut(
				relationships,
				current.get().model(),
				definition.createBuilder().toModel(masterDomainModelCreate),
				masterDomainModelCreate);
		validatePatch(definition, current.get().model(), replacementMasterDomainModel);
		definition.mutationPort().put(masterDomainId, replacementMasterDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void putNewAtId(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainId masterDomainId,
			final MasterDomainModelCreate masterDomainModelCreate)
	{
		var masterDomainModel = applyCreateRelationshipsBeforeMasterPersistence(
				relationships,
				definition.createBuilder().toModel(masterDomainModelCreate),
				masterDomainModelCreate);
		validateInsertion(definition, masterDomainModel);
		definition.mutationPort().put(masterDomainId, masterDomainModel);

		var linkedMasterDomainModel = applyCreateRelationshipsAfterMasterPersistence(
				relationships,
				masterDomainModel,
				masterDomainModelCreate);
		if (linkedMasterDomainModel != masterDomainModel)
		{
			definition.mutationPort().put(masterDomainId, linkedMasterDomainModel);
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch>
	MasterDomainModel replaceLinkedSatellitesForPut(
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainModel currentMasterDomainModel,
			final MasterDomainModel replacementMasterDomainModel,
			final MasterDomainModelCreate masterDomainModelCreate)
	{
		var linkedMasterDomainModel = replacementMasterDomainModel;
		for (var relationship : relationships)
		{
			linkedMasterDomainModel = applyPutRelationship(
					relationship,
					currentMasterDomainModel,
					linkedMasterDomainModel,
					masterDomainModelCreate);
		}
		return linkedMasterDomainModel;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validatePatch(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainModel originalMasterDomainModel,
			final MasterDomainModel replacementMasterDomainModel)
	{
		validationSupport.validateSourceAwarePatch(
				definition,
				OperationSource.USER_INTENT,
				originalMasterDomainModel,
				replacementMasterDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch>
	MasterDomainModel applyCreateRelationshipsBeforeMasterPersistence(
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainModel masterDomainModel,
			final MasterDomainModelCreate masterDomainModelCreate)
	{
		var linkedMasterDomainModel = masterDomainModel;
		for (var relationship : relationships)
		{
			if (persistsSatellitesBeforeMaster(relationship))
			{
				linkedMasterDomainModel =
						applyPutCreateRelationship(relationship, linkedMasterDomainModel, masterDomainModelCreate);
			}
		}
		return linkedMasterDomainModel;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validateInsertion(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainModel masterDomainModel)
	{
		if (!definition.securityPolicy().isAccessAllowed(masterDomainModel))
		{
			throw AccessDeniedException.withMessage("Access not allowed");
		}
		definition.insertionPolicy().validateInsertion(masterDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch>
	MasterDomainModel applyCreateRelationshipsAfterMasterPersistence(
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainModel masterDomainModel,
			final MasterDomainModelCreate masterDomainModelCreate)
	{
		var linkedMasterDomainModel = masterDomainModel;
		for (var relationship : relationships)
		{
			if (persistsSatellitesAfterMaster(relationship))
			{
				linkedMasterDomainModel =
						applyPutCreateRelationship(relationship, linkedMasterDomainModel, masterDomainModelCreate);
			}
		}
		return linkedMasterDomainModel;
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
		var targetSatelliteDomainIds =
				createIntentResolver.resolveSatelliteIdsForCreate(relationship, masterDomainModelCreate);
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

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch>
	boolean persistsSatellitesBeforeMaster(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?> relationship)
	{
		return relationship.linkStrategy().persistenceOrder() == SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER;
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
				createIntentResolver.resolveSatelliteIdsForCreate(relationship, masterDomainModelCreate));
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch>
	boolean persistsSatellitesAfterMaster(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?> relationship)
	{
		return !persistsSatellitesBeforeMaster(relationship);
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
			deleteSatellite(relationship, satelliteDomainId);
		}
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
	void deleteSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainId satelliteDomainId)
	{
		var currentSatellite = referenceResolver.requiredSatellite(relationship, satelliteDomainId);
		relationship.satelliteDefinition().deletionPolicy().validateDeletion(currentSatellite.model());
		relationship.satelliteDefinition().mutationPort().delete(satelliteDomainId);
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> IdentifiedModel<MasterDomainId, MasterDomainModel> updateById(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainId masterDomainId,
			final MasterDomainModelUpdatePatch masterDomainModelUpdatePatch)
	{
		var current = fetchRequiredMaster(definition, masterDomainId);
		var updatedMasterDomainModel = applyRelationshipUpdates(
				relationships,
				current.model(),
				definition.patcher().patchModel(current.model(), masterDomainModelUpdatePatch),
				masterDomainModelUpdatePatch);
		validatePatch(definition, current.model(), updatedMasterDomainModel);
		return definition.mutationPort().update(masterDomainId, updatedMasterDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> IdentifiedModel<MasterDomainId, MasterDomainModel> fetchRequiredMaster(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainId masterDomainId)
	{
		return definition.fetchPort().findById(masterDomainId)
		                 .orElseThrow(() -> ResourceNotFoundException.withId(masterDomainId));
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch>
	MasterDomainModel applyRelationshipUpdates(
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainModel currentMasterDomainModel,
			final MasterDomainModel updatedMasterDomainModel,
			final MasterDomainModelUpdatePatch masterDomainModelUpdatePatch)
	{
		var linkedMasterDomainModel = updatedMasterDomainModel;
		for (var relationship : relationships)
		{
			linkedMasterDomainModel = applyUpdateRelationship(
					relationship,
					currentMasterDomainModel,
					linkedMasterDomainModel,
					masterDomainModelUpdatePatch);
		}
		return linkedMasterDomainModel;
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

		validateUpdateParticipation(relationship, mutationIntents);
		return relationship.reconciliationStrategy() == ReconciliationStrategy.REPLACE
				? applyReplaceUpdateRelationship(
				relationship,
				currentMasterDomainModel,
				updatedMasterDomainModel,
				mutationIntents)
				: applyMergeByIdUpdateRelationship(
				relationship,
				currentMasterDomainModel,
				updatedMasterDomainModel,
				mutationIntents);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void validateUpdateParticipation(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch>> mutationIntents)
	{
		if (!relationship.lifecycleSemantics().cascadeUpdate())
		{
			throw AggregateRelationshipExecutionNotSupportedException.withMessage(
					"Relationship '" + relationship.name() + "' does not allow satellite update participation");
		}
		validateCurrentMutationIntentSupport(relationship, mutationIntents);
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
		var state = ReplaceUpdateState.of(currentLinkedSatelliteDomainIds(relationship, currentMasterDomainModel));
		for (var mutationIntent : mutationIntents)
		{
			handleReplaceMutationIntent(relationship, state, mutationIntent);
		}
		deleteOrphanedSatellitesIfNeeded(relationship, difference(state.current(), state.target()));
		return relationshipPlanner.replaceLinkedSatelliteDomainIds(relationship, updatedMasterDomainModel,
				state.target());
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void handleReplaceMutationIntent(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final ReplaceUpdateState<SatelliteDomainId> state,
			final SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
					mutationIntent)
	{
		switch (mutationIntent)
		{
			case SatelliteMutationIntent.ReferenceSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> referenceIntent ->
					state.keep(referenceSatellite(relationship, referenceIntent.satelliteDomainId()));
			case SatelliteMutationIntent.CreateSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> createIntent ->
					state.keep(createSatellite(relationship, createIntent.satelliteDomainModelCreate()));
			case SatelliteMutationIntent.UpdateSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> updateIntent -> state.keep(updateSatelliteAndKeepId(
					relationship,
					state.current(),
					updateIntent.satelliteDomainId(),
					updateIntent.satelliteDomainModelUpdatePatch()));
			case SatelliteMutationIntent.UpsertCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> upsertCurrentIntent -> state.keep(upsertCurrentSatellite(
					relationship,
					state.current(),
					upsertCurrentIntent.satelliteDomainModelCreate(),
					upsertCurrentIntent.satelliteDomainModelUpdatePatch()));
			case SatelliteMutationIntent.UpdateCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> updateCurrentIntent -> state.keep(updateCurrentSatellite(
					relationship,
					state.current(),
					updateCurrentIntent.satelliteDomainModelUpdatePatch()));
			case SatelliteMutationIntent.RemoveSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> _ ->
			{
			}
			case SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> _ ->
					requireOrphanDeletableCurrentSatellite(relationship, state.current());
		}
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
		var state = MergeByIdUpdateState.of(currentLinkedSatelliteDomainIds(relationship, currentMasterDomainModel));
		for (var mutationIntent : mutationIntents)
		{
			handleMergeByIdMutationIntent(relationship, state, mutationIntent);
		}
		deleteOrphanedSatellitesIfNeeded(relationship, state.removed());
		return relationshipPlanner.replaceLinkedSatelliteDomainIds(
				relationship,
				updatedMasterDomainModel,
				new ArrayList<>(state.target()));
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void handleMergeByIdMutationIntent(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MergeByIdUpdateState<SatelliteDomainId> state,
			final SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
					mutationIntent)
	{
		switch (mutationIntent)
		{
			case SatelliteMutationIntent.ReferenceSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> referenceIntent ->
					state.keep(referenceSatellite(relationship, referenceIntent.satelliteDomainId()));
			case SatelliteMutationIntent.CreateSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> createIntent ->
					state.keep(createSatellite(relationship, createIntent.satelliteDomainModelCreate()));
			case SatelliteMutationIntent.UpdateSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> updateIntent -> state.keep(updateSatelliteAndKeepId(
					relationship,
					state.current(),
					updateIntent.satelliteDomainId(),
					updateIntent.satelliteDomainModelUpdatePatch()));
			case SatelliteMutationIntent.UpsertCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> upsertCurrentIntent -> state.keep(upsertCurrentSatellite(
					relationship,
					state.current(),
					upsertCurrentIntent.satelliteDomainModelCreate(),
					upsertCurrentIntent.satelliteDomainModelUpdatePatch()));
			case SatelliteMutationIntent.UpdateCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> updateCurrentIntent -> state.keep(updateCurrentSatellite(
					relationship,
					state.current(),
					updateCurrentIntent.satelliteDomainModelUpdatePatch()));
			case SatelliteMutationIntent.RemoveSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> removeIntent -> state.remove(
					removeLinkedSatellite(relationship, state.current(), removeIntent.satelliteDomainId()));
			case SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> _ ->
					state.remove(requiredCurrentLinkedSatelliteDomainId(relationship, state.current(), "remove"));
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	List<SatelliteDomainId> currentLinkedSatelliteDomainIds(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModel currentMasterDomainModel)
	{
		return relationshipPlanner.currentLinkedSatelliteDomainIds(relationship, currentMasterDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void requireOrphanDeletableCurrentSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> currentSatelliteDomainIds)
	{
		if (!relationship.lifecycleSemantics().orphanDelete())
		{
			throw InvalidRequestException.withMessage(
					"Relationship '" + relationship.name()
							+ "' cannot remove the current satellite under REPLACE unless orphanDelete is enabled");
		}
		requiredCurrentLinkedSatelliteDomainId(relationship, currentSatelliteDomainIds, "remove");
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId referenceSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainId satelliteDomainId)
	{
		referenceResolver.requiredSatellite(relationship, satelliteDomainId);
		return satelliteDomainId;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId updateSatelliteAndKeepId(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> currentSatelliteDomainIds,
			final SatelliteDomainId satelliteDomainId,
			final SatelliteDomainModelUpdatePatch satelliteDomainModelUpdatePatch)
	{
		requireCurrentlyLinkedSatelliteDomainId(relationship, currentSatelliteDomainIds, satelliteDomainId, "update");
		updateSatellite(relationship, satelliteDomainId, satelliteDomainModelUpdatePatch);
		return satelliteDomainId;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId upsertCurrentSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> currentSatelliteDomainIds,
			final SatelliteDomainModelCreate satelliteDomainModelCreate,
			final SatelliteDomainModelUpdatePatch satelliteDomainModelUpdatePatch)
	{
		if (currentSatelliteDomainIds.isEmpty())
		{
			return createSatellite(relationship, satelliteDomainModelCreate);
		}
		return updateCurrentSatellite(relationship, currentSatelliteDomainIds, satelliteDomainModelUpdatePatch);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId updateCurrentSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> currentSatelliteDomainIds,
			final SatelliteDomainModelUpdatePatch satelliteDomainModelUpdatePatch)
	{
		var currentSatelliteDomainId =
				requiredCurrentLinkedSatelliteDomainId(relationship, currentSatelliteDomainIds, "update");
		updateSatellite(relationship, currentSatelliteDomainId, satelliteDomainModelUpdatePatch);
		return currentSatelliteDomainId;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId removeLinkedSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteDomainId> currentSatelliteDomainIds,
			final SatelliteDomainId satelliteDomainId)
	{
		requireCurrentlyLinkedSatelliteDomainId(relationship, currentSatelliteDomainIds, satelliteDomainId, "remove");
		return satelliteDomainId;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void validateCurrentMutationIntentSupport(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final Collection<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch>> mutationIntents)
	{
		if (relationship.cardinality() != Cardinality.MANY)
		{
			return;
		}
		if (mutationIntents.stream().anyMatch(mutationIntent -> mutationIntent
				instanceof SatelliteMutationIntent.UpsertCurrentSatelliteMutationIntent<?, ?, ?>
				|| mutationIntent instanceof SatelliteMutationIntent.UpdateCurrentSatelliteMutationIntent<?, ?, ?>
				|| mutationIntent instanceof SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent<?, ?, ?>))
		{
			throw InvalidRequestException.withMessage(
					"Relationship '" + relationship.name()
							+ "' cannot use implicit current satellite mutations for MANY cardinality; use explicit satellite ids instead");
		}
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
		validationSupport.validateSourceAwarePatch(
				relationship.satelliteDefinition(),
				OperationSource.USER_INTENT,
				originalSatelliteDomainModel,
				replacementSatelliteDomainModel);
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

	private AggregateUpdateCoordinator(
			final SatelliteRelationshipPlanner relationshipPlanner,
			final SatelliteReferenceResolver referenceResolver,
			final SatelliteCreateIntentResolver createIntentResolver,
			final AggregateMutationValidationSupport validationSupport)
	{
		this.relationshipPlanner = relationshipPlanner;
		this.referenceResolver = referenceResolver;
		this.createIntentResolver = createIntentResolver;
		this.validationSupport = validationSupport;
	}

	private record ReplaceUpdateState<SatelliteDomainId>(
			List<SatelliteDomainId> current,
			List<SatelliteDomainId> target)
	{
		private static <SatelliteDomainId> ReplaceUpdateState<SatelliteDomainId> of(
				final List<SatelliteDomainId> current)
		{
			return new ReplaceUpdateState<>(current, new ArrayList<>());
		}

		private void keep(final SatelliteDomainId satelliteDomainId)
		{
			target.add(satelliteDomainId);
		}
	}

	private record MergeByIdUpdateState<SatelliteDomainId>(
			List<SatelliteDomainId> current,
			LinkedHashSet<SatelliteDomainId> target,
			LinkedHashSet<SatelliteDomainId> removed)
	{
		private static <SatelliteDomainId> MergeByIdUpdateState<SatelliteDomainId> of(
				final List<SatelliteDomainId> current)
		{
			return new MergeByIdUpdateState<>(current, new LinkedHashSet<>(current), new LinkedHashSet<>());
		}

		private void keep(final SatelliteDomainId satelliteDomainId)
		{
			target.add(satelliteDomainId);
		}

		private void remove(final SatelliteDomainId satelliteDomainId)
		{
			target.remove(satelliteDomainId);
			removed.add(satelliteDomainId);
		}
	}
}