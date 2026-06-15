package de.gupta.clean.crud.template.domain.aggregate.relationship.standard;

import de.gupta.clean.crud.template.domain.aggregate.relationship.SatelliteHydrationStrategy;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

import java.util.ArrayList;

enum StandardSatelliteHydrationStrategyFactory
{
	;

	static <MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
	SatelliteHydrationStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
	defaultHydrationStrategy()
	{
		return (master, satelliteFetchPort, satelliteLinkStrategy) ->
		{
			var hydratedSatellites = new ArrayList<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>();
			for (var satelliteDomainId : satelliteLinkStrategy.currentLinkedSatelliteDomainIds(master.model()))
			{
				satelliteFetchPort.findById(satelliteDomainId).ifPresent(hydratedSatellites::add);
			}
			return satelliteLinkStrategy.attachHydratedSatellites(master.model(), hydratedSatellites);
		};
	}

}