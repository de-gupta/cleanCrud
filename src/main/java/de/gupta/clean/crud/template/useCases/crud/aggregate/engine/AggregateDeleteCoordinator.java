package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition;

import java.util.List;

final class AggregateDeleteCoordinator
{
	private final SatelliteRelationshipPlanner relationshipPlanner;
	private final SatelliteReferenceResolver referenceResolver;

	<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void deleteById(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, ?, ?, ?, ?>> relationships,
			final MasterDomainId masterDomainId)
	{
		var current = definition.fetchPort().findById(masterDomainId)
		                        .orElseThrow(() -> ResourceNotFoundException.withId(masterDomainId));
		validateMasterDeletion(definition, current.model());
		for (var relationship : relationships)
		{
			if (!relationship.lifecycleSemantics().cascadeDelete())
			{
				continue;
			}
			deleteSatellites(relationship, current.model());
		}
		definition.mutationPort().delete(masterDomainId);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			MasterDomainModelResponse> void validateMasterDeletion(
			final AggregateCrudDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
			final MasterDomainModel masterDomainModel)
	{
		if (!definition.securityPolicy().isAccessAllowed(masterDomainModel))
		{
			throw AccessDeniedException.withMessage("Access not allowed");
		}
		definition.deletionPolicy().validateDeletion(masterDomainModel);
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
			var satellite = referenceResolver.requiredSatellite(relationship, satelliteDomainId);
			relationship.satelliteDefinition().deletionPolicy().validateDeletion(satellite.model());
			relationship.satelliteDefinition().mutationPort().delete(satelliteDomainId);
		}
	}

	AggregateDeleteCoordinator(
			final SatelliteRelationshipPlanner relationshipPlanner,
			final SatelliteReferenceResolver referenceResolver)
	{
		this.relationshipPlanner = relationshipPlanner;
		this.referenceResolver = referenceResolver;
	}
}