package de.gupta.clean.crud.template.domain.aggregate.relationship;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

import java.util.Collection;
import java.util.Optional;

public interface SatelliteLinkStrategy<
		MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
{
	SatellitePersistenceOrder persistenceOrder();

	Optional<SatelliteDomainId> currentLinkedSatelliteDomainId(MasterDomainModel masterDomainModel);

	Collection<SatelliteDomainId> currentLinkedSatelliteDomainIds(MasterDomainModel masterDomainModel);

	default MasterDomainModel attachSatelliteReference(
			MasterDomainModel masterDomainModel,
			SatelliteDomainId satelliteDomainId)
	{
		return replaceLinkedSatelliteDomainIds(masterDomainModel, java.util.List.of(satelliteDomainId));
	}

	MasterDomainModel replaceLinkedSatelliteDomainIds(
			MasterDomainModel masterDomainModel,
			Collection<SatelliteDomainId> satelliteDomainIds);

	MasterDomainModel attachHydratedSatellites(
			MasterDomainModel masterDomainModel,
			Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>> satellites);
}