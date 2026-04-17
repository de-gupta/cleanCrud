package de.gupta.clean.crud.template.useCases.crud.aggregate.definition;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.lifecycle.LifecycleSemantics;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AggregateFoundationContractsTest
{
	@Test
	void cardinalityAndReconciliationStrategyExposeExpectedValues()
	{
		assertEquals(List.of(Cardinality.ONE, Cardinality.MANY), List.of(Cardinality.values()));
		assertEquals(List.of(ReconciliationStrategy.REPLACE, ReconciliationStrategy.MERGE_BY_ID),
				List.of(ReconciliationStrategy.values()));
	}

	@Test
	void aggregateCrudDefinitionCanBeInstantiatedWithFoundationContracts()
	{
		AggregateMutationPort<String, String, String, String> mutationPort = new TestAggregateMutationPort();
		AggregateFetchPort<String, String> fetchPort = new TestAggregateFetchPort();
		DomainModelBuilder<String, String> createBuilder = create -> "created:" + create;
		DomainModelPatcher<String, String> patcher = (originalDomainModel, patch) -> originalDomainModel + "|" + patch;
		DomainResponseBuilder<String, String> responseBuilder = domainModel -> "response:" + domainModel;
		InsertionPolicy<String> insertionPolicy = _ ->
		{
		};
		PatchPolicy<String> patchPolicy = (_, _) ->
		{
		};
		DeletionPolicy<String> deletionPolicy = _ ->
		{
		};
		DomainSecurityPolicy<String> securityPolicy = DomainSecurityPolicy.allowing();
		DuplicateDefinition<String> duplicateDefinition = String::equals;
		AggregateRelationshipDefinition<String, String, String, String, Long, Long, Long, Long> relationshipDefinition =
				new TestAggregateRelationshipDefinition();

		AggregateCrudDefinition<String, String, String, String, String> definition =
				new AggregateCrudDefinition<>()
				{
					@Override
					public AggregateMutationPort<String, String, String, String> mutationPort()
					{
						return mutationPort;
					}

					@Override
					public AggregateFetchPort<String, String> fetchPort()
					{
						return fetchPort;
					}

					@Override
					public DomainModelBuilder<String, String> createBuilder()
					{
						return createBuilder;
					}

					@Override
					public DomainModelPatcher<String, String> patcher()
					{
						return patcher;
					}

					@Override
					public DomainResponseBuilder<String, String> responseBuilder()
					{
						return responseBuilder;
					}

					@Override
					public InsertionPolicy<String> insertionPolicy()
					{
						return insertionPolicy;
					}

					@Override
					public PatchPolicy<String> patchPolicy()
					{
						return patchPolicy;
					}

					@Override
					public DeletionPolicy<String> deletionPolicy()
					{
						return deletionPolicy;
					}

					@Override
					public DomainSecurityPolicy<String> securityPolicy()
					{
						return securityPolicy;
					}

					@Override
					public DuplicateDefinition<String> duplicateDefinition()
					{
						return duplicateDefinition;
					}

					@Override
					public Collection<AggregateRelationshipDefinitionContract<String, String, String, String>>
					relationshipDefinitions()
					{
						return List.of(relationshipDefinition);
					}
				};

		assertSame(mutationPort, definition.mutationPort());
		assertSame(fetchPort, definition.fetchPort());
		assertSame(createBuilder, definition.createBuilder());
		assertSame(patcher, definition.patcher());
		assertSame(responseBuilder, definition.responseBuilder());
		assertSame(insertionPolicy, definition.insertionPolicy());
		assertSame(patchPolicy, definition.patchPolicy());
		assertSame(deletionPolicy, definition.deletionPolicy());
		assertSame(securityPolicy, definition.securityPolicy());
		assertSame(duplicateDefinition, definition.duplicateDefinition());
		assertEquals(List.of(relationshipDefinition), definition.relationshipDefinitions());
	}

	@Test
	void aggregateRelationshipDefinitionCanBeInstantiatedWithSatelliteContracts()
	{
		TestAggregateRelationshipDefinition definition = new TestAggregateRelationshipDefinition();

		assertEquals("satellite", definition.name());
		assertEquals(Cardinality.ONE, definition.cardinality());
		assertEquals(LifecycleSemantics.none(), definition.lifecycleSemantics());
		assertEquals(ReconciliationStrategy.REPLACE, definition.reconciliationStrategy());
		assertEquals(SatellitePersistenceOrder.NO_ORDER_CONSTRAINT, definition.linkStrategy().persistenceOrder());
	}

	private static final class TestAggregateMutationPort
			implements AggregateMutationPort<String, String, String, String>
	{
		@Override
		public IdentifiedModel<String, String> create(final String domainModel)
		{
			return IdentifiedModel.of("created", domainModel);
		}

		@Override
		public void put(final String domainId, final String domainModel)
		{
		}

		@Override
		public IdentifiedModel<String, String> update(final String domainId, final String domainModel)
		{
			return IdentifiedModel.of(domainId, domainModel);
		}

		@Override
		public void delete(final String domainId)
		{
		}
	}

	private static final class TestAggregateFetchPort implements AggregateFetchPort<String, String>
	{
		@Override
		public Optional<IdentifiedModel<String, String>> findById(final String domainId)
		{
			return Optional.of(IdentifiedModel.of(domainId, "value"));
		}

		@Override
		public Collection<IdentifiedModel<String, String>> findByIds(final Set<String> domainIds)
		{
			return domainIds.stream().map(domainId -> IdentifiedModel.of(domainId, "value")).toList();
		}

		@Override
		public Collection<IdentifiedModel<String, String>> findAll()
		{
			return List.of(IdentifiedModel.of("all", "value"));
		}

		@Override
		public Slice<IdentifiedModel<String, String>> findAll(final Pageable pageable)
		{
			return new SliceImpl<>(List.of(IdentifiedModel.of("paged", "value")));
		}
	}

	private static final class TestAggregateRelationshipDefinition
			implements AggregateRelationshipDefinition<String, String, String, String, Long, Long, Long, Long>
	{
		private final AggregateMutationPort<Long, Long, Long, Long> satelliteMutationPort =
				new TestLongAggregateMutationPort();
		private final AggregateFetchPort<Long, Long> satelliteFetchPort = new TestLongAggregateFetchPort();
		private final SatelliteCreateInputResolver<String, SatelliteCreateIntent<Long, Long>> createInputResolver =
				_ -> new SatelliteCreateIntent.NoSatelliteCreateIntent<>();
		private final SatellitePatchInputResolver<String,
				Collection<SatelliteMutationIntent<Long, Long, Long>>> patchInputResolver = _ -> List.of();
		private final SatelliteIdentityResolver<String, Long, Long> identityResolver = (_, _) -> Optional.empty();
		private final SatelliteLinkStrategy<String, String, Long, Long> linkStrategy = new TestSatelliteLinkStrategy();
		private final SatelliteHydrationStrategy<String, String, Long, Long> hydrationStrategy =
				(master, satellitePort, strategy) -> master.model();

		@Override
		public String name()
		{
			return "satellite";
		}

		@Override
		public Cardinality cardinality()
		{
			return Cardinality.ONE;
		}

		@Override
		public LifecycleSemantics lifecycleSemantics()
		{
			return LifecycleSemantics.none();
		}

		@Override
		public ReconciliationStrategy reconciliationStrategy()
		{
			return ReconciliationStrategy.REPLACE;
		}

		@Override
		public AggregateMutationPort<Long, Long, Long, Long> satelliteMutationPort()
		{
			return satelliteMutationPort;
		}

		@Override
		public AggregateFetchPort<Long, Long> satelliteFetchPort()
		{
			return satelliteFetchPort;
		}

		@Override
		public SatelliteCreateInputResolver<String, SatelliteCreateIntent<Long, Long>> createInputResolver()
		{
			return createInputResolver;
		}

		@Override
		public SatellitePatchInputResolver<String, Collection<SatelliteMutationIntent<Long, Long, Long>>>
		patchInputResolver()
		{
			return patchInputResolver;
		}

		@Override
		public SatelliteIdentityResolver<String, Long, Long> identityResolver()
		{
			return identityResolver;
		}

		@Override
		public SatelliteLinkStrategy<String, String, Long, Long> linkStrategy()
		{
			return linkStrategy;
		}

		@Override
		public SatelliteHydrationStrategy<String, String, Long, Long> hydrationStrategy()
		{
			return hydrationStrategy;
		}
	}

	private static final class TestLongAggregateMutationPort implements AggregateMutationPort<Long, Long, Long, Long>
	{
		@Override
		public IdentifiedModel<Long, Long> create(final Long domainModel)
		{
			return IdentifiedModel.of(domainModel, domainModel);
		}

		@Override
		public void put(final Long domainId, final Long domainModel)
		{
		}

		@Override
		public IdentifiedModel<Long, Long> update(final Long domainId, final Long domainModel)
		{
			return IdentifiedModel.of(domainId, domainModel);
		}

		@Override
		public void delete(final Long domainId)
		{
		}
	}

	private static final class TestLongAggregateFetchPort implements AggregateFetchPort<Long, Long>
	{
		@Override
		public Optional<IdentifiedModel<Long, Long>> findById(final Long domainId)
		{
			return Optional.of(IdentifiedModel.of(domainId, domainId));
		}

		@Override
		public Collection<IdentifiedModel<Long, Long>> findByIds(final Set<Long> domainIds)
		{
			return domainIds.stream().map(domainId -> IdentifiedModel.of(domainId, domainId)).toList();
		}

		@Override
		public Collection<IdentifiedModel<Long, Long>> findAll()
		{
			return List.of(IdentifiedModel.of(1L, 1L));
		}

		@Override
		public Slice<IdentifiedModel<Long, Long>> findAll(final Pageable pageable)
		{
			return new SliceImpl<>(List.of(IdentifiedModel.of(1L, 1L)));
		}
	}

	private static final class TestSatelliteLinkStrategy implements SatelliteLinkStrategy<String, String, Long, Long>
	{
		@Override
		public SatellitePersistenceOrder persistenceOrder()
		{
			return SatellitePersistenceOrder.NO_ORDER_CONSTRAINT;
		}

		@Override
		public Optional<Long> currentLinkedSatelliteDomainId(final String masterDomainModel)
		{
			return Optional.empty();
		}

		@Override
		public Collection<Long> currentLinkedSatelliteDomainIds(final String masterDomainModel)
		{
			return List.of();
		}

		@Override
		public String attachSatelliteReference(final String masterDomainModel, final Long satelliteDomainId)
		{
			return masterDomainModel + satelliteDomainId;
		}

		@Override
		public String attachHydratedSatellites(
				final String masterDomainModel,
				final Collection<IdentifiedModel<Long, Long>> satellites)
		{
			return masterDomainModel + satellites.size();
		}
	}
}
