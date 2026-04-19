package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.SatellitePersistenceOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class AggregateSaveCoordinator
{
	private final SatelliteRelationshipPlanner relationshipPlanner;
	private final SatelliteReferenceResolver referenceResolver;

	<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> saveAll(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final Collection<MasterDomainModelCreate> models)
	{
		var savedModels = new ArrayList<IdentifiedModel<MasterDomainId, MasterDomainModel>>();
		for (var model : models)
		{
			savedModels.add(save(definition, relationships, model));
		}
		return savedModels;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> IdentifiedModel<MasterDomainId, MasterDomainModel> save(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainModelCreate masterDomainModelCreate)
	{
		MasterDomainModel masterDomainModel = definition.createBuilder().toModel(masterDomainModelCreate);

		for (var relationship : relationships)
		{
			if (relationship.linkStrategy().persistenceOrder() == SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER)
			{
				masterDomainModel = applySaveRelationship(relationship, masterDomainModel, masterDomainModelCreate);
			}
		}

		validateMasterForSave(definition, masterDomainModel);
		IdentifiedModel<MasterDomainId, MasterDomainModel> savedMaster =
				definition.mutationPort().create(masterDomainModel);
		MasterDomainModel linkedMasterDomainModel = savedMaster.model();

		for (var relationship : relationships)
		{
			if (relationship.linkStrategy().persistenceOrder() != SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER)
			{
				linkedMasterDomainModel =
						applySaveRelationship(relationship, linkedMasterDomainModel, masterDomainModelCreate);
			}
		}

		if (linkedMasterDomainModel != savedMaster.model())
		{
			savedMaster = definition.mutationPort().update(savedMaster.id(), linkedMasterDomainModel);
		}
		return savedMaster;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	MasterDomainModel applySaveRelationship(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModel masterDomainModel,
			final MasterDomainModelCreate masterDomainModelCreate)
	{
		List<SatelliteDomainId> satelliteDomainIds =
				resolveSatelliteIdsForCreate(relationship, masterDomainModelCreate);
		return relationshipPlanner.replaceLinkedSatelliteDomainIds(relationship, masterDomainModel, satelliteDomainIds);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	List<SatelliteDomainId> resolveSatelliteIdsForCreate(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModelCreate masterDomainModelCreate)
	{
		var satelliteDomainIds = new ArrayList<SatelliteDomainId>();
		for (var intent : relationshipPlanner.createIntents(relationship, masterDomainModelCreate))
		{
			switch (intent)
			{
				case SatelliteCreateIntent.ReferenceSatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate> reference ->
				{
					referenceResolver.requiredSatellite(relationship, reference.satelliteDomainId());
					satelliteDomainIds.add(reference.satelliteDomainId());
				}
				case SatelliteCreateIntent.InlineSatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate> inline ->
				{
					if (!relationship.lifecycleSemantics().cascadeCreate())
					{
						throw AggregateRelationshipExecutionNotSupportedException.withMessage(
								"Relationship '" + relationship.name() + "' does not allow satellite create participation");
					}
					var satelliteDomainModel = relationship.satelliteDefinition().createBuilder()
					                                       .toModel(inline.satelliteDomainModelCreate());
					validateSatelliteForCreate(relationship, satelliteDomainModel);
					satelliteDomainIds.add(relationship.satelliteDefinition()
					                                   .mutationPort()
					                                   .create(satelliteDomainModel)
					                                   .id());
				}
				case SatelliteCreateIntent.NoSatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate> ignored ->
				{
				}
			}
		}
		return satelliteDomainIds;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validateMasterForSave(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainModel masterDomainModel)
	{
		if (!definition.securityPolicy().isAccessAllowed(masterDomainModel))
		{
			throw AccessDeniedException.withMessage("Access not allowed for one or more models");
		}
		definition.insertionPolicy().validateInsertion(masterDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void validateSatelliteForCreate(
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

	AggregateSaveCoordinator(
			final SatelliteRelationshipPlanner relationshipPlanner,
			final SatelliteReferenceResolver referenceResolver)
	{
		this.relationshipPlanner = relationshipPlanner;
		this.referenceResolver = referenceResolver;
	}
}