package de.gupta.clean.crud.template.domain.aggregate.intent;

public sealed interface SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>
		permits SatelliteCreateIntent.NoSatelliteCreateIntent,
		SatelliteCreateIntent.ReferenceSatelliteCreateIntent,
		SatelliteCreateIntent.InlineSatelliteCreateIntent
{
	record NoSatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>()
			implements SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>
	{
	}

	record ReferenceSatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>(
			SatelliteDomainId satelliteDomainId)
			implements SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>
	{
	}

	record InlineSatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>(
			SatelliteDomainModelCreate satelliteDomainModelCreate)
			implements SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>
	{
	}
}