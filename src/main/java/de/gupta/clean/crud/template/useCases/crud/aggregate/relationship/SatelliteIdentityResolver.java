package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship;

import java.util.Optional;

/**
 * Resolves the satellite domain ID represented by a master/satellite pairing.
 * <p>
 * This contract is primarily for declaration clarity and future orchestration refinements. Current many-relationship
 * merge semantics rely on explicit IDs carried by mutation intents.
 */
@FunctionalInterface
public interface SatelliteIdentityResolver<MasterDomainModel, SatelliteDomainModel, SatelliteDomainId>
{
	Optional<SatelliteDomainId> resolveSatelliteDomainId(
			MasterDomainModel masterDomainModel,
			SatelliteDomainModel satelliteDomainModel);
}
