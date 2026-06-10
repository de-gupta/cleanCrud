package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

public final class AggregateServiceSupportFactory
{
	private static final AggregateDefinitionGuard DEFINITION_GUARD = new AggregateDefinitionGuard();
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

	public static AggregateDefinitionGuard definitionGuard()
	{
		return DEFINITION_GUARD;
	}

	public static AggregateMutationValidationSupport validationSupport()
	{
		return VALIDATION_SUPPORT;
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

	private AggregateServiceSupportFactory()
	{
	}
}
