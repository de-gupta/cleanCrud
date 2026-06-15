package de.gupta.clean.crud.template.domain.service.aggregate.mechanics.coordinator;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.relationship.SatelliteReferenceResolver;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.relationship.SatelliteRelationshipPlanner;

import java.util.List;

public final class AggregateDeleteCoordinator
{
	private final SatelliteRelationshipPlanner relationshipPlanner;
	private final SatelliteReferenceResolver referenceResolver;

	public static AggregateDeleteCoordinator with(
			final SatelliteRelationshipPlanner relationshipPlanner,
			final SatelliteReferenceResolver referenceResolver)
	{
		return new AggregateDeleteCoordinator(relationshipPlanner, referenceResolver);
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void deleteById(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainId masterDomainId)
	{
		var current = fetchRequiredMaster(definition, masterDomainId);
		validateMasterDeletion(definition, current.model());
		deleteCascadingSatellites(relationships, current.model());
		definition.mutationPort().delete(masterDomainId);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel<MasterDomainId, MasterDomainModel> fetchRequiredMaster(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainId masterDomainId)
	{
		return definition.fetchPort().findById(masterDomainId)
		                 .orElseThrow(() -> ResourceNotFoundException.withId(masterDomainId));
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validateMasterDeletion(
			final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainModel masterDomainModel)
	{
		if (!definition.securityPolicy().isAccessAllowed(masterDomainModel))
		{
			throw AccessDeniedException.withMessage("Access not allowed");
		}
		definition.deletionPolicy().validateDeletion(masterDomainModel);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch>
	void deleteCascadingSatellites(
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainModel masterDomainModel)
	{
		for (var relationship : relationships)
		{
			if (relationship.lifecycleSemantics().cascadeDelete())
			{
				deleteSatellites(relationship, masterDomainModel);
			}
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void deleteSatellites(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModel masterDomainModel)
	{
		for (var satelliteDomainId : relationshipPlanner.currentLinkedSatelliteDomainIds(relationship,
				masterDomainModel))
		{
			deleteSatellite(relationship, satelliteDomainId);
		}
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void deleteSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainId satelliteDomainId)
	{
		var satellite = referenceResolver.requiredSatellite(relationship, satelliteDomainId);
		relationship.satelliteDefinition().deletionPolicy().validateDeletion(satellite.model());
		relationship.satelliteDefinition().mutationPort().delete(satelliteDomainId);
	}

	private AggregateDeleteCoordinator(
			final SatelliteRelationshipPlanner relationshipPlanner,
			final SatelliteReferenceResolver referenceResolver)
	{
		this.relationshipPlanner = relationshipPlanner;
		this.referenceResolver = referenceResolver;
	}
}