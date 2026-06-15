package de.gupta.clean.crud.template.domain.aggregate.relationship;

import de.gupta.clean.crud.template.domain.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

public interface SatelliteHydrationStrategy<
		MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
{
	MasterDomainModel hydrate(
			IdentifiedModel<MasterDomainId, MasterDomainModel> master,
			AggregateFetchPort<SatelliteDomainId, SatelliteDomainModel> satelliteFetchPort,
			SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
					satelliteLinkStrategy);
}