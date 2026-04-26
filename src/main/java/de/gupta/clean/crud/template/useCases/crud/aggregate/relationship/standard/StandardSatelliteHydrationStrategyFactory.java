package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.standard;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.SatelliteHydrationStrategy;

import java.util.ArrayList;

final class StandardSatelliteHydrationStrategyFactory
{
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

	private StandardSatelliteHydrationStrategyFactory()
	{
	}
}
