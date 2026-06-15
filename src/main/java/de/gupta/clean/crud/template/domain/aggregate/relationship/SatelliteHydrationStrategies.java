package de.gupta.clean.crud.template.domain.aggregate.relationship;

import de.gupta.clean.crud.template.domain.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

public enum SatelliteHydrationStrategies
{
	;

	public static <MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
	SatelliteHydrationStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel> none()
	{
		return new NoOpSatelliteHydrationStrategy<>();
	}

	private record NoOpSatelliteHydrationStrategy<
			MasterDomainId,
			MasterDomainModel,
			SatelliteDomainId,
			SatelliteDomainModel>()
			implements SatelliteHydrationStrategy<MasterDomainId,
			MasterDomainModel,
			SatelliteDomainId,
			SatelliteDomainModel>
	{
		@Override
		public MasterDomainModel hydrate(
				final IdentifiedModel<MasterDomainId, MasterDomainModel> master,
				final AggregateFetchPort<SatelliteDomainId, SatelliteDomainModel> satelliteFetchPort,
				final SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel> satelliteLinkStrategy)
		{
			return master.model();
		}
	}
}