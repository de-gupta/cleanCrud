package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

import java.util.Collection;
import java.util.Optional;

public interface SatelliteLinkStrategy<
		MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
{
	SatellitePersistenceOrder persistenceOrder();

	Optional<SatelliteDomainId> currentLinkedSatelliteDomainId(MasterDomainModel masterDomainModel);

	Collection<SatelliteDomainId> currentLinkedSatelliteDomainIds(MasterDomainModel masterDomainModel);

	MasterDomainModel replaceLinkedSatelliteDomainIds(
			MasterDomainModel masterDomainModel,
			Collection<SatelliteDomainId> satelliteDomainIds);

	default MasterDomainModel attachSatelliteReference(
			MasterDomainModel masterDomainModel,
			SatelliteDomainId satelliteDomainId)
	{
		return replaceLinkedSatelliteDomainIds(masterDomainModel, java.util.List.of(satelliteDomainId));
	}

	MasterDomainModel attachHydratedSatellites(
			MasterDomainModel masterDomainModel,
			Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>> satellites);
}
