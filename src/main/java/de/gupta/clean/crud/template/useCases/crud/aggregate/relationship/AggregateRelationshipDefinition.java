package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship;

import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;

import java.util.Collection;

public interface AggregateRelationshipDefinition<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch,
		SatelliteDomainId,
		SatelliteDomainModel,
		SatelliteDomainModelCreate,
		SatelliteDomainModelUpdatePatch>
		extends AggregateRelationshipDefinitionContract<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch>
{
	AggregateMutationPort<SatelliteDomainId,
			SatelliteDomainModel,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch> satelliteMutationPort();

	AggregateFetchPort<SatelliteDomainId, SatelliteDomainModel> satelliteFetchPort();

	@Override
	SatelliteCreateInputResolver<MasterDomainModelCreate,
			SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>> createInputResolver();

	@Override
	SatellitePatchInputResolver<MasterDomainModelUpdatePatch,
			Collection<SatelliteMutationIntent<SatelliteDomainId,
					SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch>>> patchInputResolver();

	SatelliteIdentityResolver<MasterDomainModel, SatelliteDomainModel, SatelliteDomainId> identityResolver();

	SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel> linkStrategy();

	SatelliteHydrationStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel> hydrationStrategy();
}