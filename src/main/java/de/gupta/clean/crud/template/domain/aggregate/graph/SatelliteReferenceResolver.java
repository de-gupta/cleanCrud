package de.gupta.clean.crud.template.domain.aggregate.graph;

import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

public final class SatelliteReferenceResolver
{
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	IdentifiedModel<SatelliteDomainId, SatelliteDomainModel> requiredSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationshipDefinition,
			final SatelliteDomainId satelliteDomainId)
	{
		IdentifiedModel<SatelliteDomainId, SatelliteDomainModel> identifiedModel =
				relationshipDefinition.satelliteDefinition()
				                      .fetchPort()
				                      .findById(satelliteDomainId)
				                      .orElseThrow(() -> ResourceNotFoundException.withId(satelliteDomainId));
		if (!relationshipDefinition.satelliteDefinition().securityPolicy().isAccessAllowed(identifiedModel.model()))
		{
			throw AccessDeniedException.withMessage("Access not allowed");
		}
		return identifiedModel;
	}
}