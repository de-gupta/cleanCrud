package de.gupta.clean.crud.template.domain.aggregate.execution;

import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;

@FunctionalInterface
public interface SatelliteCreateValidator
{
	<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void validate(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainModel satelliteDomainModel);
}