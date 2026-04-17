package de.gupta.clean.crud.template.useCases.crud.aggregate.relationship;

import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;

import java.util.Collection;

/**
 * Executable aggregate relationship contract that provides the runtime collaborators required for satellite
 * orchestration.
 */
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
	AggregateCrudDefinition<SatelliteDomainId,
			SatelliteDomainModel,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch,
			?> satelliteDefinition();

	AggregateMutationPort<SatelliteDomainId,
			SatelliteDomainModel,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch> satelliteMutationPort();

	AggregateFetchPort<SatelliteDomainId, SatelliteDomainModel> satelliteFetchPort();

	@Override
	SatelliteCreateInputResolver<MasterDomainModelCreate,
			Collection<SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>>> createInputResolver();

	@Override
	SatellitePatchInputResolver<MasterDomainModelUpdatePatch,
			Collection<SatelliteMutationIntent<SatelliteDomainId,
					SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch>>> patchInputResolver();

	/**
	 * Optional identity helper for relating master and satellite domain models.
	 * <p>
	 * Runtime reconciliation for {@link ReconciliationStrategy#MERGE_BY_ID} uses explicit IDs from mutation intents.
	 */
	SatelliteIdentityResolver<MasterDomainModel, SatelliteDomainModel, SatelliteDomainId> identityResolver();

	SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel> linkStrategy();

	SatelliteHydrationStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel> hydrationStrategy();
}
