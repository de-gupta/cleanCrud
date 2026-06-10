package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.SatellitePersistenceOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class AggregateSaveCoordinator
{
	private final SatelliteRelationshipPlanner relationshipPlanner;
	private final SatelliteCreateIntentResolver createIntentResolver;

	public static AggregateSaveCoordinator with(final SatelliteRelationshipPlanner relationshipPlanner,
	                                            final SatelliteCreateIntentResolver createIntentResolver)
	{
		return new AggregateSaveCoordinator(relationshipPlanner, createIntentResolver);
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> saveAll(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final Collection<MasterDomainModelCreate> models)
	{
		return saveAll(
				definition,
				relationships,
				models,
				masterDomainModel -> validateMasterForSave(definition, masterDomainModel),
				createIntentResolver);
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> saveAll(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final Collection<MasterDomainModelCreate> models,
			final AggregateCreateValidator<MasterDomainModel> masterCreateValidator,
			final SatelliteCreateIntentResolver satelliteCreateIntentResolver)
	{
		var savedModels = new ArrayList<IdentifiedModel<MasterDomainId, MasterDomainModel>>();
		for (var model : models)
		{
			savedModels.add(
					save(definition, relationships, model, masterCreateValidator, satelliteCreateIntentResolver));
		}
		return savedModels;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> IdentifiedModel<MasterDomainId, MasterDomainModel> save(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainModelCreate masterDomainModelCreate,
			final AggregateCreateValidator<MasterDomainModel> masterCreateValidator,
			final SatelliteCreateIntentResolver satelliteCreateIntentResolver)
	{
		var masterDomainModel = applyRelationshipsBeforeMasterPersistence(
				relationships,
				definition.createBuilder().toModel(masterDomainModelCreate),
				masterDomainModelCreate,
				satelliteCreateIntentResolver);
		masterCreateValidator.validate(masterDomainModel);
		var savedMaster = definition.mutationPort().create(masterDomainModel);
		var linkedMasterDomainModel =
				applyRelationshipsAfterMasterPersistence(
						relationships,
						savedMaster.model(),
						masterDomainModelCreate,
						satelliteCreateIntentResolver);
		if (masterWasRelinked(savedMaster, linkedMasterDomainModel))
		{
			savedMaster = definition.mutationPort().update(savedMaster.id(), linkedMasterDomainModel);
		}
		return savedMaster;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch> MasterDomainModel applyRelationshipsBeforeMasterPersistence(
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainModel masterDomainModel,
			final MasterDomainModelCreate masterDomainModelCreate,
			final SatelliteCreateIntentResolver satelliteCreateIntentResolver)
	{
		var linkedMasterDomainModel = masterDomainModel;
		for (var relationship : relationships)
		{
			if (persistsSatellitesBeforeMaster(relationship))
			{
				linkedMasterDomainModel = applySaveRelationship(
						relationship,
						linkedMasterDomainModel,
						masterDomainModelCreate,
						satelliteCreateIntentResolver);
			}
		}
		return linkedMasterDomainModel;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch> MasterDomainModel applyRelationshipsAfterMasterPersistence(
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainModel masterDomainModel,
			final MasterDomainModelCreate masterDomainModelCreate,
			final SatelliteCreateIntentResolver satelliteCreateIntentResolver)
	{
		var linkedMasterDomainModel = masterDomainModel;
		for (var relationship : relationships)
		{
			if (persistsSatellitesAfterMaster(relationship))
			{
				linkedMasterDomainModel = applySaveRelationship(
						relationship,
						linkedMasterDomainModel,
						masterDomainModelCreate,
						satelliteCreateIntentResolver);
			}
		}
		return linkedMasterDomainModel;
	}

	private <MasterDomainId, MasterDomainModel> boolean masterWasRelinked(
			final IdentifiedModel<MasterDomainId, MasterDomainModel> savedMaster,
			final MasterDomainModel linkedMasterDomainModel)
	{
		return linkedMasterDomainModel != savedMaster.model();
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch> MasterDomainModel applySaveRelationship(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModel masterDomainModel,
			final MasterDomainModelCreate masterDomainModelCreate,
			final SatelliteCreateIntentResolver satelliteCreateIntentResolver)
	{
		List<SatelliteDomainId> satelliteDomainIds =
				satelliteCreateIntentResolver.resolveSatelliteIdsForCreate(relationship, masterDomainModelCreate);
		return relationshipPlanner.replaceLinkedSatelliteDomainIds(relationship, masterDomainModel, satelliteDomainIds);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> void validateMasterForSave(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainModel masterDomainModel)
	{
		if (!definition.securityPolicy().isAccessAllowed(masterDomainModel))
		{
			throw AccessDeniedException.withMessage("Access not allowed for one or more models");
		}
		definition.insertionPolicy().validateInsertion(masterDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch> boolean persistsSatellitesBeforeMaster(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, ?, ?, ?, ?> relationship)
	{
		return relationship.linkStrategy().persistenceOrder() == SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch> boolean persistsSatellitesAfterMaster(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, ?, ?, ?, ?> relationship)
	{
		return !persistsSatellitesBeforeMaster(relationship);
	}

	private AggregateSaveCoordinator(final SatelliteRelationshipPlanner relationshipPlanner,
	                                 final SatelliteCreateIntentResolver createIntentResolver)
	{
		this.relationshipPlanner = relationshipPlanner;
		this.createIntentResolver = createIntentResolver;
	}
}