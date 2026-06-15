package de.gupta.clean.crud.template.domain.aggregate.intent;

public sealed interface SatelliteMutationIntent<
		SatelliteDomainId,
		SatelliteDomainModelCreate,
		SatelliteDomainModelUpdatePatch>
		permits SatelliteMutationIntent.ReferenceSatelliteMutationIntent,
		SatelliteMutationIntent.CreateSatelliteMutationIntent,
		SatelliteMutationIntent.UpdateSatelliteMutationIntent,
		SatelliteMutationIntent.UpsertCurrentSatelliteMutationIntent,
		SatelliteMutationIntent.UpdateCurrentSatelliteMutationIntent,
		SatelliteMutationIntent.RemoveSatelliteMutationIntent,
		SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent
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

	record UpsertCurrentSatelliteMutationIntent<
			SatelliteDomainId,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>(
			SatelliteDomainModelCreate satelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch satelliteDomainModelUpdatePatch)
			implements SatelliteMutationIntent<
			SatelliteDomainId,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>
	{
	}

	record UpdateCurrentSatelliteMutationIntent<
			SatelliteDomainId,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>(
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

	record RemoveCurrentSatelliteMutationIntent<
			SatelliteDomainId,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>()
			implements SatelliteMutationIntent<
			SatelliteDomainId,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>
	{
	}
}