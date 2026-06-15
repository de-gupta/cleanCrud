package de.gupta.clean.crud.template.domain.aggregate.graph;

import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;

public final class SatelliteCreateIntentResolver
{
	private final SatelliteRelationshipPlanner relationshipPlanner;
	private final SatelliteReferenceResolver referenceResolver;
	private final SatelliteCreateValidator satelliteCreateValidator;

	public static SatelliteCreateIntentResolver with(
			final SatelliteRelationshipPlanner relationshipPlanner,
			final SatelliteReferenceResolver referenceResolver)
	{
		return with(relationshipPlanner, referenceResolver, SatelliteCreateIntentResolver::validateSatelliteForCreate);
	}

	public static SatelliteCreateIntentResolver with(
			final SatelliteRelationshipPlanner relationshipPlanner,
			final SatelliteReferenceResolver referenceResolver,
			final SatelliteCreateValidator satelliteCreateValidator)
	{
		return new SatelliteCreateIntentResolver(relationshipPlanner, referenceResolver, satelliteCreateValidator);
	}

	private static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	void validateSatelliteForCreate(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainModel satelliteDomainModel)
	{
		if (!relationship.satelliteDefinition().securityPolicy().isAccessAllowed(satelliteDomainModel))
		{
			throw AccessDeniedException.withMessage("Access not allowed");
		}
		relationship.satelliteDefinition().insertionPolicy().validateInsertion(satelliteDomainModel);
	}

	public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	List<SatelliteDomainId> resolveSatelliteIdsForCreate(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final MasterDomainModelCreate masterDomainModelCreate)
	{
		var satelliteDomainIds = new ArrayList<SatelliteDomainId>();
		for (var createIntent : relationshipPlanner.createIntents(relationship, masterDomainModelCreate))
		{
			switch (createIntent)
			{
				case SatelliteCreateIntent.ReferenceSatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate> referenceIntent ->
						satelliteDomainIds.add(referenceSatellite(relationship, referenceIntent.satelliteDomainId()));
				case SatelliteCreateIntent.InlineSatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate> inlineIntent ->
						satelliteDomainIds.add(
								createInlineSatellite(relationship, inlineIntent.satelliteDomainModelCreate()));
				case SatelliteCreateIntent.NoSatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate> _ ->
				{
				}
			}
		}
		return satelliteDomainIds;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId referenceSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainId satelliteDomainId)
	{
		referenceResolver.requiredSatellite(relationship, satelliteDomainId);
		return satelliteDomainId;
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId createInlineSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainModelCreate satelliteDomainModelCreate)
	{
		if (!relationship.lifecycleSemantics().cascadeCreate())
		{
			throw AggregateRelationshipExecutionNotSupportedException.withMessage(
					"Relationship '" + relationship.name() + "' does not allow satellite create participation");
		}
		return createSatellite(relationship, satelliteDomainModelCreate);
	}

	private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	SatelliteDomainId createSatellite(
			final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
					MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> relationship,
			final SatelliteDomainModelCreate satelliteDomainModelCreate)
	{
		var satelliteDomainModel =
				relationship.satelliteDefinition().createBuilder().toModel(satelliteDomainModelCreate);
		satelliteCreateValidator.validate(relationship, satelliteDomainModel);
		return relationship.satelliteDefinition().mutationPort().create(satelliteDomainModel).id();
	}

	private SatelliteCreateIntentResolver(
			final SatelliteRelationshipPlanner relationshipPlanner,
			final SatelliteReferenceResolver referenceResolver,
			final SatelliteCreateValidator satelliteCreateValidator)
	{
		this.relationshipPlanner = relationshipPlanner;
		this.referenceResolver = referenceResolver;
		this.satelliteCreateValidator = satelliteCreateValidator;
	}
}