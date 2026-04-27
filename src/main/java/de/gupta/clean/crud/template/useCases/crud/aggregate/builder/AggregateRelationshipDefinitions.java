package de.gupta.clean.crud.template.useCases.crud.aggregate.builder;

import de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
import de.gupta.clean.crud.template.domain.relationship.Relationship;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.*;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.standard.*;

import java.util.Collection;
import java.util.Objects;

public final class AggregateRelationshipDefinitions
{
	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	AggregateRelationshipDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch> aggregateRelationshipDefinition()
	{
		return new AggregateRelationshipDefinitionBuilder<>();
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
	RelationshipDrivenAggregateRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch> fromRelationship(
			final Relationship relationship,
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch, ?> satelliteDefinition)
	{
		return new RelationshipDrivenAggregateRelationshipBuilder<>(relationship, satelliteDefinition);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch,
			SatelliteAggregateResponse, SatellitePublicCreate, SatellitePublicUpdatePatch, SatellitePublicResponse>
	StandardOneToOneSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> oneToOneSatellite(
			final String name,
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse> satelliteDefinition)
	{
		return new StandardOneToOneSatelliteRelationshipBuilder<>(name, satelliteDefinition);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch,
			SatelliteAggregateResponse, SatellitePublicResponse>
	StandardOneToOneReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicResponse>
	oneToOneReferencedSatellite(
			final String name,
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse> satelliteDefinition)
	{
		return new StandardOneToOneReferencedSatelliteRelationshipBuilder<>(name, satelliteDefinition);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch,
			SatelliteAggregateResponse, SatellitePublicCreate, SatellitePublicUpdatePatch, SatellitePublicResponse>
	StandardOneToManySatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicCreate,
			SatellitePublicUpdatePatch, SatellitePublicResponse> oneToManySatellite(
			final String name,
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse> satelliteDefinition)
	{
		return new StandardOneToManySatelliteRelationshipBuilder<>(name, satelliteDefinition);
	}

	public static <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
			SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch,
			SatelliteAggregateResponse, SatellitePublicResponse>
	StandardOneToManyReferencedSatelliteRelationshipBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
			MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse, SatellitePublicResponse>
	oneToManyReferencedSatellite(
			final String name,
			final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch, SatelliteAggregateResponse> satelliteDefinition)
	{
		return new StandardOneToManyReferencedSatelliteRelationshipBuilder<>(name, satelliteDefinition);
	}

	private static <Value> Value required(final Value value, final String name)
	{
		return Objects.requireNonNull(value, name);
	}

	private AggregateRelationshipDefinitions()
	{
	}

	public static final class AggregateRelationshipDefinitionBuilder<
			MasterDomainId,
			MasterDomainModel,
			MasterDomainModelCreate,
			MasterDomainModelUpdatePatch,
			SatelliteDomainId,
			SatelliteDomainModel,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>
	{
		private String name;
		private Cardinality cardinality;
		private LifecycleSemantics lifecycleSemantics = LifecycleSemantics.none();
		private AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch, ?> satelliteDefinition;
		private AggregateMutationPort<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> satelliteMutationPort;
		private AggregateFetchPort<SatelliteDomainId, SatelliteDomainModel> satelliteFetchPort;
		private SatelliteCreateInputResolver<MasterDomainModelCreate,
				Collection<SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>>> createInputResolver;
		private SatellitePatchInputResolver<MasterDomainModelUpdatePatch,
				Collection<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch>>> patchInputResolver;
		private SatelliteIdentityResolver<MasterDomainModel, SatelliteDomainModel, SatelliteDomainId> identityResolver;
		private ReconciliationStrategy reconciliationStrategy;
		private SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
				linkStrategy;
		private SatelliteHydrationStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
				hydrationStrategy;

		public AggregateRelationshipDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> name(final String name)
		{
			this.name = name;
			return this;
		}

		public AggregateRelationshipDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> cardinality(final Cardinality cardinality)
		{
			this.cardinality = cardinality;
			return this;
		}

		public AggregateRelationshipDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> lifecycleSemantics(final LifecycleSemantics lifecycleSemantics)
		{
			this.lifecycleSemantics = lifecycleSemantics;
			return this;
		}

		public AggregateRelationshipDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> satelliteDefinition(
				final AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch, ?> satelliteDefinition)
		{
			this.satelliteDefinition = satelliteDefinition;
			return this;
		}

		public AggregateRelationshipDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> satelliteMutationPort(
				final AggregateMutationPort<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> satelliteMutationPort)
		{
			this.satelliteMutationPort = satelliteMutationPort;
			return this;
		}

		public AggregateRelationshipDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> satelliteFetchPort(
				final AggregateFetchPort<SatelliteDomainId, SatelliteDomainModel> satelliteFetchPort)
		{
			this.satelliteFetchPort = satelliteFetchPort;
			return this;
		}

		public AggregateRelationshipDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> createInputResolver(
				final SatelliteCreateInputResolver<MasterDomainModelCreate,
						Collection<SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>>>
						createInputResolver)
		{
			this.createInputResolver = createInputResolver;
			return this;
		}

		public AggregateRelationshipDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> patchInputResolver(
				final SatellitePatchInputResolver<MasterDomainModelUpdatePatch,
						Collection<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
								SatelliteDomainModelUpdatePatch>>> patchInputResolver)
		{
			this.patchInputResolver = patchInputResolver;
			return this;
		}

		public AggregateRelationshipDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> identityResolver(
				final SatelliteIdentityResolver<MasterDomainModel, SatelliteDomainModel, SatelliteDomainId>
						identityResolver)
		{
			this.identityResolver = identityResolver;
			return this;
		}

		public AggregateRelationshipDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> reconciliationStrategy(
				final ReconciliationStrategy reconciliationStrategy)
		{
			this.reconciliationStrategy = reconciliationStrategy;
			return this;
		}

		public AggregateRelationshipDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> linkStrategy(
				final SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
						linkStrategy)
		{
			this.linkStrategy = linkStrategy;
			return this;
		}

		public AggregateRelationshipDefinitionBuilder<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> hydrationStrategy(
				final SatelliteHydrationStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId,
						SatelliteDomainModel> hydrationStrategy)
		{
			this.hydrationStrategy = hydrationStrategy;
			return this;
		}

		public AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> build()
		{
			var requiredSatelliteDefinition = required(satelliteDefinition, "satelliteDefinition");
			var effectiveSatelliteMutationPort =
					satelliteMutationPort == null ? requiredSatelliteDefinition.mutationPort() : satelliteMutationPort;
			var effectiveSatelliteFetchPort =
					satelliteFetchPort == null ? requiredSatelliteDefinition.fetchPort() : satelliteFetchPort;
			return new BuiltAggregateRelationshipDefinition<>(
					required(name, "name"),
					required(cardinality, "cardinality"),
					required(lifecycleSemantics, "lifecycleSemantics"),
					requiredSatelliteDefinition,
					effectiveSatelliteMutationPort,
					effectiveSatelliteFetchPort,
					required(createInputResolver, "createInputResolver"),
					required(patchInputResolver, "patchInputResolver"),
					required(identityResolver, "identityResolver"),
					required(reconciliationStrategy, "reconciliationStrategy"),
					required(linkStrategy, "linkStrategy"),
					hydrationStrategy == null && !lifecycleSemantics.hydrateOnFetch()
							? SatelliteHydrationStrategies.none()
							: required(hydrationStrategy, "hydrationStrategy"));
		}

		private AggregateRelationshipDefinitionBuilder()
		{
		}
	}

	private record BuiltAggregateRelationshipDefinition<
			MasterDomainId,
			MasterDomainModel,
			MasterDomainModelCreate,
			MasterDomainModelUpdatePatch,
			SatelliteDomainId,
			SatelliteDomainModel,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>(
			String name,
			Cardinality cardinality,
			LifecycleSemantics lifecycleSemantics,
			AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch, ?> satelliteDefinition,
			AggregateMutationPort<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch> satelliteMutationPort,
			AggregateFetchPort<SatelliteDomainId, SatelliteDomainModel> satelliteFetchPort,
			SatelliteCreateInputResolver<MasterDomainModelCreate,
					Collection<SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>>>
			createInputResolver,
			SatellitePatchInputResolver<MasterDomainModelUpdatePatch,
					Collection<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
							SatelliteDomainModelUpdatePatch>>> patchInputResolver,
			SatelliteIdentityResolver<MasterDomainModel, SatelliteDomainModel, SatelliteDomainId> identityResolver,
			ReconciliationStrategy reconciliationStrategy,
			SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
			linkStrategy,
			SatelliteHydrationStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
			hydrationStrategy)
			implements AggregateRelationshipDefinition<MasterDomainId,
			MasterDomainModel,
			MasterDomainModelCreate,
			MasterDomainModelUpdatePatch,
			SatelliteDomainId,
			SatelliteDomainModel,
			SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch>
	{
	}
}