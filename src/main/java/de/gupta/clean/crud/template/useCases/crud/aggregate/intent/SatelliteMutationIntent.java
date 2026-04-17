package de.gupta.clean.crud.template.useCases.crud.aggregate.intent;

public sealed interface SatelliteMutationIntent<
		SatelliteDomainId,
		SatelliteDomainModelCreate,
		SatelliteDomainModelUpdatePatch>
		permits SatelliteMutationIntent.ReferenceSatelliteMutationIntent,
		SatelliteMutationIntent.CreateSatelliteMutationIntent, SatelliteMutationIntent.UpdateSatelliteMutationIntent,
		SatelliteMutationIntent.RemoveSatelliteMutationIntent
{
	record ReferenceSatelliteMutationIntent<
			SatelliteDomainId,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>(
			SatelliteDomainId satelliteDomainId)
			implements SatelliteMutationIntent<
			SatelliteDomainId,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>
	{
	}

	record CreateSatelliteMutationIntent<
			SatelliteDomainId,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>(
			SatelliteDomainModelCreate satelliteDomainModelCreate)
			implements SatelliteMutationIntent<
			SatelliteDomainId,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>
	{
	}

	record UpdateSatelliteMutationIntent<
			SatelliteDomainId,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>(
			SatelliteDomainId satelliteDomainId,
			SatelliteDomainModelUpdatePatch satelliteDomainModelUpdatePatch)
			implements SatelliteMutationIntent<
			SatelliteDomainId,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>
	{
	}

	record RemoveSatelliteMutationIntent<
			SatelliteDomainId,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>(
			SatelliteDomainId satelliteDomainId)
			implements SatelliteMutationIntent<
			SatelliteDomainId,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>
	{
	}
}