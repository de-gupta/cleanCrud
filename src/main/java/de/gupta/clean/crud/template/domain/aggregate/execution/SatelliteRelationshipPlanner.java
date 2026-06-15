package de.gupta.clean.crud.template.domain.aggregate.execution;

import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.domain.aggregate.relationship.Cardinality;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SatelliteRelationshipPlanner
{
	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	List<SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>> createIntents(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationshipDefinition,
			final MasterDomainModelCreate masterDomainModelCreate)
	{
		List<SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>> intents =
				relationshipDefinition.createInputResolver()
				                      .resolveSatelliteCreateIntent(masterDomainModelCreate)
				                      .stream()
				                      .map(this::<SatelliteDomainId, SatelliteDomainModelCreate>castCreateIntent)
				                      .filter(intent -> !(intent instanceof SatelliteCreateIntent.NoSatelliteCreateIntent<?, ?>))
				                      .toList();
		validateCardinality(relationshipDefinition.cardinality(), intents.size(), "create");
		return intents;
	}

	@SuppressWarnings("unchecked")
	private <SatelliteDomainId, SatelliteDomainModelCreate>
	SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate> castCreateIntent(
			final SatelliteCreateIntent<?, ?> intent)
	{
		return (SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>) intent;
	}

	private void validateCardinality(final Cardinality cardinality, final int elementCount, final String operation)
	{
		if (cardinality == Cardinality.ONE && elementCount > 1)
		{
			throw InvalidRequestException.withMessage(
					"Only one satellite intent is allowed for a ONE relationship during " + operation);
		}
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	List<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>>
	mutationIntents(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationshipDefinition,
			final MasterDomainModelUpdatePatch masterDomainModelUpdatePatch)
	{
		List<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>>
				intents =
				relationshipDefinition.patchInputResolver()
				                      .resolveSatelliteMutationIntents(masterDomainModelUpdatePatch)
				                      .stream()
				                      .map(this::<SatelliteDomainId,
											  SatelliteDomainModelCreate,
											  SatelliteDomainModelUpdatePatch>castMutationIntent)
				                      .toList();
		validateCardinality(relationshipDefinition.cardinality(), intents.size(), "update");
		return intents;
	}

	@SuppressWarnings("unchecked")
	private <SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	castMutationIntent(final SatelliteMutationIntent<?, ?, ?> intent)
	{
		return (SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>)
				intent;
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	List<SatelliteDomainId> currentLinkedSatelliteDomainIds(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationshipDefinition,
			final MasterDomainModel masterDomainModel)
	{
		List<SatelliteDomainId> currentSatelliteDomainIds =
				relationshipDefinition.linkStrategy().currentLinkedSatelliteDomainIds(masterDomainModel).stream()
				                      .toList();
		if (relationshipDefinition.cardinality() == Cardinality.ONE && currentSatelliteDomainIds.size() > 1)
		{
			throw AggregateRelationshipExecutionNotSupportedException.withMessage(
					"Cardinality.ONE relationship '" + relationshipDefinition.name() +
							"' resolved more than one current satellite");
		}
		return currentSatelliteDomainIds;
	}

	public <MasterDomainModel, SatelliteDomainId>
	MasterDomainModel replaceLinkedSatelliteDomainIds(
			final AggregateRelationshipDefinition<?, MasterDomainModel, ?, ?, SatelliteDomainId, ?, ?, ?> relationshipDefinition,
			final MasterDomainModel masterDomainModel,
			final Collection<SatelliteDomainId> satelliteDomainIds)
	{
		validateCardinality(relationshipDefinition.cardinality(), satelliteDomainIds.size(), "link");
		return relationshipDefinition.linkStrategy().replaceLinkedSatelliteDomainIds(masterDomainModel,
				new ArrayList<>(satelliteDomainIds));
	}
}