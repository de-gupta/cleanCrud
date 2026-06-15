package de.gupta.clean.crud.template.domain.aggregate.relationship;

@FunctionalInterface
public interface SatellitePatchInputResolver<MasterDomainModelUpdatePatch, SatelliteMutationIntentModels>
{
	SatelliteMutationIntentModels resolveSatelliteMutationIntents(
			MasterDomainModelUpdatePatch masterDomainModelUpdatePatch);
}