package de.gupta.clean.crud.template.domain.service.aggregate.mechanics;

import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.coordinator.AggregateDeleteCoordinator;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.coordinator.AggregateFetchCoordinator;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.coordinator.AggregateSaveCoordinator;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.coordinator.AggregateUpdateCoordinator;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.intent.SatelliteCreateIntentResolver;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.relationship.AggregateDefinitionRelationshipInspector;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.relationship.SatelliteReferenceResolver;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.relationship.SatelliteRelationshipPlanner;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.validator.AggregateMutationValidationSupport;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.validator.SatelliteCreateValidator;

public enum AggregateServiceSupportFactory
{
	;
	private static final AggregateDefinitionRelationshipInspector
			RELATIONSHIP_INSPECTOR = new AggregateDefinitionRelationshipInspector();
	private static final AggregateMutationValidationSupport VALIDATION_SUPPORT =
			new AggregateMutationValidationSupport();
	private static final SatelliteRelationshipPlanner RELATIONSHIP_PLANNER = new SatelliteRelationshipPlanner();
	private static final SatelliteReferenceResolver REFERENCE_RESOLVER = new SatelliteReferenceResolver();
	private static final SatelliteCreateIntentResolver CREATE_INTENT_RESOLVER =
			SatelliteCreateIntentResolver.with(RELATIONSHIP_PLANNER, REFERENCE_RESOLVER);
	private static final AggregateSaveCoordinator SAVE_COORDINATOR =
			AggregateSaveCoordinator.with(RELATIONSHIP_PLANNER, CREATE_INTENT_RESOLVER);
	private static final AggregateUpdateCoordinator UPDATE_COORDINATOR =
			AggregateUpdateCoordinator.with(RELATIONSHIP_PLANNER, REFERENCE_RESOLVER, CREATE_INTENT_RESOLVER,
					VALIDATION_SUPPORT);
	private static final AggregateFetchCoordinator FETCH_COORDINATOR = AggregateFetchCoordinator.create();
	private static final AggregateDeleteCoordinator DELETE_COORDINATOR =
			AggregateDeleteCoordinator.with(RELATIONSHIP_PLANNER, REFERENCE_RESOLVER);

	public static AggregateDefinitionRelationshipInspector definitionGuard()
	{
		return RELATIONSHIP_INSPECTOR;
	}

	public static AggregateMutationValidationSupport validationSupport()
	{
		return VALIDATION_SUPPORT;
	}

	public static SatelliteRelationshipPlanner relationshipPlanner()
	{
		return RELATIONSHIP_PLANNER;
	}

	public static SatelliteReferenceResolver referenceResolver()
	{
		return REFERENCE_RESOLVER;
	}

	public static AggregateSaveCoordinator saveCoordinator()
	{
		return SAVE_COORDINATOR;
	}

	public static SatelliteCreateIntentResolver satelliteCreateIntentResolver(
			final SatelliteCreateValidator satelliteCreateValidator)
	{
		return SatelliteCreateIntentResolver.with(RELATIONSHIP_PLANNER, REFERENCE_RESOLVER, satelliteCreateValidator);
	}

	public static AggregateFetchCoordinator fetchCoordinator()
	{
		return FETCH_COORDINATOR;
	}

	public static AggregateDeleteCoordinator deleteCoordinator()
	{
		return DELETE_COORDINATOR;
	}

	public static AggregateUpdateCoordinator updateCoordinator()
	{
		return UPDATE_COORDINATOR;
	}

}