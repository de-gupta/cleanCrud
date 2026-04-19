package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship;

@FunctionalInterface
public interface SatellitePatchInputResolver<MasterDomainModelUpdatePatch, SatelliteMutationIntentModels>
{
	SatelliteMutationIntentModels resolveSatelliteMutationIntents(
			MasterDomainModelUpdatePatch masterDomainModelUpdatePatch);
}
