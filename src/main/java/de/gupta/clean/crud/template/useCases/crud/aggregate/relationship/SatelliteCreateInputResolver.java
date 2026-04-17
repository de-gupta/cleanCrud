package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship;

@FunctionalInterface
public interface SatelliteCreateInputResolver<MasterDomainModelCreate, SatelliteCreateIntentModel>
{
	SatelliteCreateIntentModel resolveSatelliteCreateIntent(MasterDomainModelCreate masterDomainModelCreate);
}
