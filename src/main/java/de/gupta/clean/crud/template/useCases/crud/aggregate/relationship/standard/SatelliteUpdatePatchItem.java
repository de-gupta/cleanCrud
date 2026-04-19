package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.standard;

import java.util.Optional;

public record SatelliteUpdatePatchItem<SatelliteDomainId, SatelliteUpdatePatch>(
		Optional<SatelliteDomainId> id,
		SatelliteUpdatePatch patch)
{
	public static <SatelliteDomainId, SatelliteUpdatePatch>
	SatelliteUpdatePatchItem<SatelliteDomainId, SatelliteUpdatePatch> of(
			final Optional<SatelliteDomainId> id,
			final SatelliteUpdatePatch patch)
	{
		return new SatelliteUpdatePatchItem<>(id, patch);
	}

	public SatelliteUpdatePatchItem
	{
		id = Optional.ofNullable(id).orElse(Optional.empty());
	}
}