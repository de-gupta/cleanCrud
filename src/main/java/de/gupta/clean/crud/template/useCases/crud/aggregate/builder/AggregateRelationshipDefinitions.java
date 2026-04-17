package de.gupta.clean.crud.template.useCases.crud.aggregate.builder;

import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.lifecycle.LifecycleSemantics;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.*;

import java.util.Collection;
import java.util.Objects;

/**
 * Entry point for fluent {@link AggregateRelationshipDefinition} construction.
 */
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
			var requiredName = Objects.requireNonNull(name, "name");
			var requiredCardinality = Objects.requireNonNull(cardinality, "cardinality");
			var requiredLifecycleSemantics = Objects.requireNonNull(lifecycleSemantics, "lifecycleSemantics");
			var requiredSatelliteDefinition = Objects.requireNonNull(satelliteDefinition, "satelliteDefinition");
			var effectiveSatelliteMutationPort =
					satelliteMutationPort == null ? requiredSatelliteDefinition.mutationPort() : satelliteMutationPort;
			var effectiveSatelliteFetchPort =
					satelliteFetchPort == null ? requiredSatelliteDefinition.fetchPort() : satelliteFetchPort;
			var requiredCreateInputResolver = Objects.requireNonNull(createInputResolver, "createInputResolver");
			var requiredPatchInputResolver = Objects.requireNonNull(patchInputResolver, "patchInputResolver");
			var requiredIdentityResolver = Objects.requireNonNull(identityResolver, "identityResolver");
			var requiredReconciliationStrategy =
					Objects.requireNonNull(reconciliationStrategy, "reconciliationStrategy");
			var requiredLinkStrategy = Objects.requireNonNull(linkStrategy, "linkStrategy");
			var requiredHydrationStrategy = Objects.requireNonNull(hydrationStrategy, "hydrationStrategy");

			return new AggregateRelationshipDefinition<>()
			{
				@Override
				public String name()
				{
					return requiredName;
				}

				@Override
				public Cardinality cardinality()
				{
					return requiredCardinality;
				}

				@Override
				public LifecycleSemantics lifecycleSemantics()
				{
					return requiredLifecycleSemantics;
				}

				@Override
				public ReconciliationStrategy reconciliationStrategy()
				{
					return requiredReconciliationStrategy;
				}

				@Override
				public AggregateCrudDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch, ?> satelliteDefinition()
				{
					return requiredSatelliteDefinition;
				}

				@Override
				public AggregateMutationPort<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
						SatelliteDomainModelUpdatePatch> satelliteMutationPort()
				{
					return effectiveSatelliteMutationPort;
				}

				@Override
				public AggregateFetchPort<SatelliteDomainId, SatelliteDomainModel> satelliteFetchPort()
				{
					return effectiveSatelliteFetchPort;
				}

				@Override
				public SatelliteCreateInputResolver<MasterDomainModelCreate,
						Collection<SatelliteCreateIntent<SatelliteDomainId, SatelliteDomainModelCreate>>>
				createInputResolver()
				{
					return requiredCreateInputResolver;
				}

				@Override
				public SatellitePatchInputResolver<MasterDomainModelUpdatePatch,
						Collection<SatelliteMutationIntent<SatelliteDomainId, SatelliteDomainModelCreate,
								SatelliteDomainModelUpdatePatch>>> patchInputResolver()
				{
					return requiredPatchInputResolver;
				}

				@Override
				public SatelliteIdentityResolver<MasterDomainModel, SatelliteDomainModel, SatelliteDomainId>
				identityResolver()
				{
					return requiredIdentityResolver;
				}

				@Override
				public SatelliteLinkStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId, SatelliteDomainModel>
				linkStrategy()
				{
					return requiredLinkStrategy;
				}

				@Override
				public SatelliteHydrationStrategy<MasterDomainId, MasterDomainModel, SatelliteDomainId,
						SatelliteDomainModel> hydrationStrategy()
				{
					return requiredHydrationStrategy;
				}
			};
		}

		private AggregateRelationshipDefinitionBuilder()
		{
		}
	}
}