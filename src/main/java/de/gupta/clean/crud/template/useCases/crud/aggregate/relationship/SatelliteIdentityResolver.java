package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship;

import java.util.Optional;

@FunctionalInterface
public interface SatelliteIdentityResolver<MasterDomainModel, SatelliteDomainModel, SatelliteDomainId>
{
	Optional<SatelliteDomainId> resolveSatelliteDomainId(
			MasterDomainModel masterDomainModel,
			SatelliteDomainModel satelliteDomainModel);
}
