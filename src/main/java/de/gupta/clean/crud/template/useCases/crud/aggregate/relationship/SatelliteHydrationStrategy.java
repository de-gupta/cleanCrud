package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;

public interface SatelliteHydrationStrategy<
		MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
{
	MasterDomainModel hydrate(
			IdentifiedModel<MasterDomainId, MasterDomainModel> master,
			AggregateFetchPort<SatelliteDomainId, SatelliteDomainModel> satelliteFetchPort,
			SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
					satelliteLinkStrategy);
}
