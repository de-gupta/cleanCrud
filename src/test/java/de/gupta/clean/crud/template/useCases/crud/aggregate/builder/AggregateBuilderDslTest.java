package de.gupta.clean.crud.template.useCases.crud.aggregate.builder;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.equality.KeyBasedDuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.DefaultAggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.lifecycle.LifecycleSemantics;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.*;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.AbstractFetchService;
import de.gupta.clean.crud.template.useCases.crud.save.application.service.AbstractSaveService;
import de.gupta.clean.crud.template.useCases.crud.update.application.service.AbstractUpdateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Aggregate builder DSL tests")
final class AggregateBuilderDslTest
{
	@org.junit.jupiter.api.Test
	void relationshipDefaultsToNoOpHydrationStrategyWhenHydrationOnFetchIsDisabled()
	{
		var scenario = new TestScenario();
		var relationshipDefinition = AggregateRelationshipDefinitions
				.<String, MasterModel, MasterCreate, MasterPatch, Long, SatelliteModel, SatelliteCreate, SatellitePatch>aggregateRelationshipDefinition()
				.name("satellite")
				.cardinality(Cardinality.ONE)
				.lifecycleSemantics(LifecycleSemantics.none())
				.satelliteDefinition(scenario.satelliteDefinition())
				.createInputResolver(MasterCreate::satelliteCreateIntents)
				.patchInputResolver(MasterPatch::satelliteMutationIntents)
				.identityResolver((_, _) -> Optional.empty())
				.reconciliationStrategy(ReconciliationStrategy.REPLACE)
				.linkStrategy(scenario.linkStrategy(SatellitePersistenceOrder.NO_ORDER_CONSTRAINT))
				.build();

		var hydrated = relationshipDefinition.hydrationStrategy().hydrate(
				IdentifiedModel.of("master-1", new MasterModel("master", List.of(1L), List.of())),
				new AggregateFetchPort<>()
				{
					@Override
					public Optional<IdentifiedModel<Long, SatelliteModel>> findById(final Long domainId)
					{
						throw new AssertionError("No-op hydration strategy should not fetch satellites");
					}

					@Override
					public Collection<IdentifiedModel<Long, SatelliteModel>> findByIds(final Set<Long> domainIds)
					{
						return List.of();
					}

					@Override
					public Collection<IdentifiedModel<Long, SatelliteModel>> findAll()
					{
						return List.of();
					}

					@Override
					public Slice<IdentifiedModel<Long, SatelliteModel>> findAll(final Pageable pageable)
					{
						return new SliceImpl<>(List.of());
					}
				},
				scenario.linkStrategy(SatellitePersistenceOrder.NO_ORDER_CONSTRAINT));

		assertThat(hydrated)
				.as("No-op hydration should return the unchanged master model")
				.isEqualTo(new MasterModel("master", List.of(1L), List.of()));
	}

	@org.junit.jupiter.api.Test
	void relationshipStillRequiresHydrationStrategyWhenHydrationOnFetchIsEnabled()
	{
		var scenario = new TestScenario();
		assertThatThrownBy(() -> AggregateRelationshipDefinitions
				.<String, MasterModel, MasterCreate, MasterPatch, Long, SatelliteModel, SatelliteCreate, SatellitePatch>aggregateRelationshipDefinition()
				.name("satellite")
				.cardinality(Cardinality.ONE)
				.lifecycleSemantics(LifeCycleBuilders.oneToOneLifecycle())
				.satelliteDefinition(scenario.satelliteDefinition())
				.createInputResolver(MasterCreate::satelliteCreateIntents)
				.patchInputResolver(MasterPatch::satelliteMutationIntents)
				.identityResolver((_, _) -> Optional.empty())
				.reconciliationStrategy(ReconciliationStrategy.REPLACE)
				.linkStrategy(scenario.linkStrategy(SatellitePersistenceOrder.NO_ORDER_CONSTRAINT))
				.build())
				.as("Hydrating relationships should still require an explicit hydration strategy")
				.isInstanceOf(NullPointerException.class)
				.hasMessage("hydrationStrategy");
	}

	private record MasterCreate(
			String value,
			Collection<SatelliteCreateIntent<Long, SatelliteCreate>> satelliteCreateIntents)
	{
	}

	private record MasterPatch(
			String value,
			Collection<SatelliteMutationIntent<Long, SatelliteCreate, SatellitePatch>> satelliteMutationIntents)
	{
	}

	private record MasterModel(
			String value,
			List<Long> satelliteDomainIds,
			List<SatelliteModel> hydratedSatellites)
	{
		private MasterModel withSatelliteDomainIds(final Collection<Long> newSatelliteDomainIds)
		{
			return new MasterModel(value, List.copyOf(newSatelliteDomainIds), hydratedSatellites);
		}

		private MasterModel withHydratedSatellites(final Collection<IdentifiedModel<Long, SatelliteModel>> satellites)
		{
			return new MasterModel(value, satelliteDomainIds, satellites.stream().map(IdentifiedModel::model).toList());
		}
	}

	private record MasterResponse(String value, List<Long> satelliteDomainIds, List<String> hydratedSatelliteValues)
	{
		private static MasterResponse from(final MasterModel masterDomainModel)
		{
			return new MasterResponse(masterDomainModel.value(), masterDomainModel.satelliteDomainIds(),
					masterDomainModel.hydratedSatellites().stream().map(SatelliteModel::value).toList());
		}
	}

	private record SatelliteCreate(String value)
	{
	}

	private record SatellitePatch(String value)
	{
	}

	private record SatelliteModel(String value)
	{
	}

	private static final class TestScenario
	{
		private final Map<String, MasterModel> masterStore = new LinkedHashMap<>();
		private final Map<Long, SatelliteModel> satelliteStore = new LinkedHashMap<>();
		private final AggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		private final AtomicInteger generatedMasterIds = new AtomicInteger();
		private final AtomicInteger generatedSatelliteIds = new AtomicInteger();
		private final AggregateCrudDefinition<Long, SatelliteModel, SatelliteCreate, SatellitePatch, String>
				satelliteDefinition = satelliteDefinition();
		private AggregateCrudDefinition<String, MasterModel, MasterCreate, MasterPatch, MasterResponse>
				masterDefinition =
				masterDefinition(List.of());

		private void installRelationship(
				final Cardinality cardinality,
				final ReconciliationStrategy reconciliationStrategy,
				final LifecycleSemantics lifecycleSemantics,
				final SatellitePersistenceOrder satellitePersistenceOrder)
		{
			var relationshipDefinition = AggregateRelationshipDefinitions
					.<String, MasterModel, MasterCreate, MasterPatch, Long, SatelliteModel, SatelliteCreate, SatellitePatch>
							aggregateRelationshipDefinition()
					.name("satellite")
					.cardinality(cardinality)
					.lifecycleSemantics(lifecycleSemantics)
					.satelliteDefinition(satelliteDefinition)
					.createInputResolver(MasterCreate::satelliteCreateIntents)
					.patchInputResolver(MasterPatch::satelliteMutationIntents)
					.identityResolver((_, _) -> Optional.empty())
					.reconciliationStrategy(reconciliationStrategy)
					.linkStrategy(linkStrategy(satellitePersistenceOrder))
					.hydrationStrategy((master, satelliteFetchPort, satelliteLinkStrategy) ->
					{
						var hydratedSatellites = new ArrayList<IdentifiedModel<Long, SatelliteModel>>();
						for (var satelliteDomainId : satelliteLinkStrategy.currentLinkedSatelliteDomainIds(
								master.model()))
						{
							satelliteFetchPort.findById(satelliteDomainId).ifPresent(hydratedSatellites::add);
						}
						return satelliteLinkStrategy.attachHydratedSatellites(master.model(), hydratedSatellites);
					})
					.build();
			masterDefinition = masterDefinition(List.of(relationshipDefinition));
		}

		private AggregateCrudDefinition<String, MasterModel, MasterCreate, MasterPatch, MasterResponse> masterDefinition(
				final Collection<? extends AggregateRelationshipDefinitionContract<String, MasterModel, MasterCreate, MasterPatch>>
						relationships)
		{
			return AggregateCrudDefinitions
					.<String, MasterModel, MasterCreate, MasterPatch, MasterResponse>aggregateCrudDefinition()
					.mutationPort(masterMutationPort())
					.fetchPort(masterFetchPort())
					.createBuilder(create -> new MasterModel(create.value(), List.of(), List.of()))
					.patcher((originalDomainModel, patch) -> new MasterModel(
							patch.value() == null ? originalDomainModel.value() : patch.value(),
							originalDomainModel.satelliteDomainIds(),
							originalDomainModel.hydratedSatellites()))
					.responseBuilder(MasterResponse::from)
					.insertionPolicy(_ ->
					{
					})
					.patchPolicy((_, _) ->
					{
					})
					.deletionPolicy(_ ->
					{
					})
					.securityPolicy(DomainSecurityPolicy.allowing())
					.duplicateDefinition((KeyBasedDuplicateDefinition<MasterModel, String>) MasterModel::value)
					.relationshipDefinitions(relationships)
					.build();
		}

		private TestSaveService saveService()
		{
			return new TestSaveService(masterDefinition, engine);
		}

		private TestUpdateService updateService()
		{
			return new TestUpdateService(masterDefinition, engine);
		}

		private TestFetchService fetchService()
		{
			return new TestFetchService(masterDefinition, engine);
		}

		private AggregateCrudDefinition<Long, SatelliteModel, SatelliteCreate, SatellitePatch, String> satelliteDefinition()
		{
			return AggregateCrudDefinitions
					.<Long, SatelliteModel, SatelliteCreate, SatellitePatch, String>aggregateCrudDefinition()
					.mutationPort(satelliteMutationPort())
					.fetchPort(satelliteFetchPort())
					.createBuilder(create -> new SatelliteModel(create.value()))
					.patcher((_, patch) -> new SatelliteModel(patch.value()))
					.responseBuilder(SatelliteModel::value)
					.insertionPolicy(_ ->
					{
					})
					.patchPolicy((_, _) ->
					{
					})
					.deletionPolicy(_ ->
					{
					})
					.securityPolicy(DomainSecurityPolicy.allowing())
					.duplicateDefinition((left, right) -> left.value().equals(right.value()))
					.build();
		}

		private AggregateMutationPort<String, MasterModel, MasterCreate, MasterPatch> masterMutationPort()
		{
			return new AggregateMutationPort<>()
			{
				@Override
				public IdentifiedModel<String, MasterModel> create(final MasterModel domainModel)
				{
					var masterDomainId = "master-" + generatedMasterIds.incrementAndGet();
					masterStore.put(masterDomainId, domainModel);
					return IdentifiedModel.of(masterDomainId, domainModel);
				}

				@Override
				public void put(final String masterDomainId, final MasterModel domainModel)
				{
					masterStore.put(masterDomainId, domainModel);
				}

				@Override
				public IdentifiedModel<String, MasterModel> update(final String masterDomainId,
				                                                   final MasterModel domainModel)
				{
					masterStore.put(masterDomainId, domainModel);
					return IdentifiedModel.of(masterDomainId, domainModel);
				}

				@Override
				public void delete(final String masterDomainId)
				{
					masterStore.remove(masterDomainId);
				}
			};
		}

		private AggregateFetchPort<String, MasterModel> masterFetchPort()
		{
			return new AggregateFetchPort<>()
			{
				@Override
				public Optional<IdentifiedModel<String, MasterModel>> findById(final String masterDomainId)
				{
					return Optional.ofNullable(masterStore.get(masterDomainId))
					               .map(model -> IdentifiedModel.of(masterDomainId, model));
				}

				@Override
				public Collection<IdentifiedModel<String, MasterModel>> findByIds(final Set<String> masterDomainIds)
				{
					return masterDomainIds.stream().flatMap(masterDomainId -> findById(masterDomainId).stream())
					                      .toList();
				}

				@Override
				public Collection<IdentifiedModel<String, MasterModel>> findAll()
				{
					return masterStore.entrySet().stream()
					                  .map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue())).toList();
				}

				@Override
				public Slice<IdentifiedModel<String, MasterModel>> findAll(final Pageable pageable)
				{
					return new SliceImpl<>(findAll().stream().toList());
				}
			};
		}

		private AggregateMutationPort<Long, SatelliteModel, SatelliteCreate, SatellitePatch> satelliteMutationPort()
		{
			return new AggregateMutationPort<>()
			{
				@Override
				public IdentifiedModel<Long, SatelliteModel> create(final SatelliteModel domainModel)
				{
					generatedSatelliteIds.updateAndGet(current -> Math.max(current, satelliteStore.keySet().stream()
					                                                                              .mapToInt(
																										  Long::intValue)
					                                                                              .max()
					                                                                              .orElse(0)));
					var satelliteDomainId = (long) generatedSatelliteIds.incrementAndGet();
					satelliteStore.put(satelliteDomainId, domainModel);
					return IdentifiedModel.of(satelliteDomainId, domainModel);
				}

				@Override
				public void put(final Long satelliteDomainId, final SatelliteModel domainModel)
				{
					satelliteStore.put(satelliteDomainId, domainModel);
				}

				@Override
				public IdentifiedModel<Long, SatelliteModel> update(final Long satelliteDomainId,
				                                                    final SatelliteModel domainModel)
				{
					satelliteStore.put(satelliteDomainId, domainModel);
					return IdentifiedModel.of(satelliteDomainId, domainModel);
				}

				@Override
				public void delete(final Long satelliteDomainId)
				{
					satelliteStore.remove(satelliteDomainId);
				}
			};
		}

		private AggregateFetchPort<Long, SatelliteModel> satelliteFetchPort()
		{
			return new AggregateFetchPort<>()
			{
				@Override
				public Optional<IdentifiedModel<Long, SatelliteModel>> findById(final Long satelliteDomainId)
				{
					return Optional.ofNullable(satelliteStore.get(satelliteDomainId))
					               .map(model -> IdentifiedModel.of(satelliteDomainId, model));
				}

				@Override
				public Collection<IdentifiedModel<Long, SatelliteModel>> findByIds(final Set<Long> satelliteDomainIds)
				{
					return satelliteDomainIds.stream()
					                         .flatMap(satelliteDomainId -> findById(satelliteDomainId).stream())
					                         .toList();
				}

				@Override
				public Collection<IdentifiedModel<Long, SatelliteModel>> findAll()
				{
					return satelliteStore.entrySet().stream()
					                     .map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue())).toList();
				}

				@Override
				public Slice<IdentifiedModel<Long, SatelliteModel>> findAll(final Pageable pageable)
				{
					return new SliceImpl<>(findAll().stream().toList());
				}
			};
		}

		private SatelliteLinkStrategy<String, MasterModel, Long, SatelliteModel> linkStrategy(
				final SatellitePersistenceOrder satellitePersistenceOrder)
		{
			return new SatelliteLinkStrategy<>()
			{
				@Override
				public SatellitePersistenceOrder persistenceOrder()
				{
					return satellitePersistenceOrder;
				}

				@Override
				public Optional<Long> currentLinkedSatelliteDomainId(final MasterModel masterDomainModel)
				{
					return masterDomainModel.satelliteDomainIds().stream().findFirst();
				}

				@Override
				public Collection<Long> currentLinkedSatelliteDomainIds(final MasterModel masterDomainModel)
				{
					return masterDomainModel.satelliteDomainIds();
				}

				@Override
				public MasterModel replaceLinkedSatelliteDomainIds(
						final MasterModel masterDomainModel,
						final Collection<Long> satelliteDomainIds)
				{
					return masterDomainModel.withSatelliteDomainIds(satelliteDomainIds);
				}

				@Override
				public MasterModel attachHydratedSatellites(
						final MasterModel masterDomainModel,
						final Collection<IdentifiedModel<Long, SatelliteModel>> satellites)
				{
					return masterDomainModel.withHydratedSatellites(satellites);
				}
			};
		}
	}

	private static final class TestSaveService
			extends AbstractSaveService<String, MasterModel, MasterCreate, MasterPatch, MasterResponse>
	{
		private TestSaveService(
				final AggregateCrudDefinition<String, MasterModel, MasterCreate, MasterPatch, MasterResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine);
		}
	}

	private static final class TestUpdateService
			extends AbstractUpdateService<String, MasterModel, MasterCreate, MasterPatch, MasterResponse>
	{
		private TestUpdateService(
				final AggregateCrudDefinition<String, MasterModel, MasterCreate, MasterPatch, MasterResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine);
		}
	}

	private static final class TestFetchService
			extends AbstractFetchService<String, MasterModel, MasterCreate, MasterPatch, MasterResponse>
	{
		private TestFetchService(
				final AggregateCrudDefinition<String, MasterModel, MasterCreate, MasterPatch, MasterResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine);
		}
	}

	private static final class InlineTransactionRunner implements PersistenceTransactionRunner
	{
		@Override
		public <T> T inTransaction(final Supplier<T> action)
		{
			return action.get();
		}
	}

	private static final class LifeCycleBuilders
	{
		private static LifecycleSemantics oneToOneLifecycle()
		{
			return LifecycleSemanticsBuilder.lifecycleSemantics()
			                                .cascadeCreate()
			                                .cascadeUpdate()
			                                .hydrateOnFetch()
			                                .build();
		}

		private static LifecycleSemantics manyLifecycle()
		{
			return LifecycleSemanticsBuilder.lifecycleSemantics()
			                                .cascadeCreate()
			                                .cascadeUpdate()
			                                .cascadeDelete()
			                                .orphanDelete()
			                                .hydrateOnFetch()
			                                .build();
		}

		private LifeCycleBuilders()
		{
		}
	}

	@Nested
	@DisplayName("Lifecycle semantics builder")
	final class LifecycleSemanticsBuilderTests
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("builderCases")
		@DisplayName("lifecycle builder should produce explicit defaults and toggles")
		void lifecycleBuilderShouldProduceExplicitDefaultsAndToggles(final String as, final LifecycleCase tc)
		{
			assertThat(tc.builder().get().build())
					.as("lifecycle semantics should match for %s", as)
					.isEqualTo(tc.expected());
		}

		private static Stream<Arguments> builderCases()
		{
			return Stream.of(
					LifecycleCase.shape("default flags", LifecycleSemanticsBuilder::lifecycleSemantics,
							LifecycleSemantics.none()),
					LifecycleCase.shape("all flags enabled",
							() -> LifecycleSemanticsBuilder.lifecycleSemantics()
							                               .cascadeCreate()
							                               .cascadeUpdate()
							                               .cascadeDelete()
							                               .orphanDelete()
							                               .hydrateOnFetch(),
							LifecycleSemantics.of(true, true, true, true, true))
			).map(tc -> Arguments.of(tc.as(), tc));
		}

		private record LifecycleCase(
				String as,
				Supplier<LifecycleSemanticsBuilder> builder,
				LifecycleSemantics expected)
		{
			private static LifecycleCase shape(
					final String as,
					final Supplier<LifecycleSemanticsBuilder> builder,
					final LifecycleSemantics expected)
			{
				return new LifecycleCase(as, builder, expected);
			}
		}
	}

	@Nested
	@DisplayName("Aggregate and relationship builders")
	final class AggregateAndRelationshipBuilderTests
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("builderCases")
		@DisplayName("builder-produced definitions should drive the real engine and services")
		void builderProducedDefinitionsShouldDriveTheRealEngineAndServices(final String as, final BuilderCase tc)
		{
			var scenario = new TestScenario();
			tc.configure().accept(scenario);

			assertThat(tc.assertion().apply(scenario))
					.as("builder-driven behavior should match for %s", as)
					.isTrue();
		}

		private static Stream<Arguments> builderCases()
		{
			return Stream.of(
					BuilderCase.shape(
							"zero-relationship aggregate definition",
							_ ->
							{
							},
							scenario ->
							{
								var saved = scenario.saveService().save(new MasterCreate("master", List.of()));
								return saved.model().value().equals("master");
							}),
					BuilderCase.shape(
							"one-to-one relationship built through the DSL",
							scenario -> scenario.installRelationship(
									Cardinality.ONE,
									ReconciliationStrategy.REPLACE,
									LifeCycleBuilders.oneToOneLifecycle(),
									SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER),
							scenario ->
							{
								var saved = scenario.saveService().save(new MasterCreate(
										"master",
										List.of(new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
												new SatelliteCreate("satellite")))));
								var fetched = scenario.fetchService().findById(saved.id());
								return fetched.model().satelliteDomainIds().equals(List.of(1L))
										&& fetched.model().hydratedSatellites().stream()
										          .map(SatelliteModel::value)
										          .toList()
										          .equals(List.of("satellite"));
							}),
					BuilderCase.shape(
							"many MERGE_BY_ID relationship built through the DSL",
							scenario ->
							{
								scenario.installRelationship(
										Cardinality.MANY,
										ReconciliationStrategy.MERGE_BY_ID,
										LifeCycleBuilders.manyLifecycle(),
										SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
								scenario.masterStore.put("master-1",
										new MasterModel("master", List.of(1L, 2L), List.of()));
								scenario.satelliteStore.put(1L, new SatelliteModel("one"));
								scenario.satelliteStore.put(2L, new SatelliteModel("two"));
							},
							scenario ->
							{
								var updated = scenario.updateService().updateById(
										"master-1",
										new MasterPatch(
												null,
												List.of(
														new SatelliteMutationIntent.UpdateSatelliteMutationIntent<>(1L,
																new SatellitePatch("one-updated")),
														new SatelliteMutationIntent.RemoveSatelliteMutationIntent<>(2L),
														new SatelliteMutationIntent.CreateSatelliteMutationIntent<>(
																new SatelliteCreate("three")))));
								var fetched = scenario.fetchService().findById("master-1");
								return updated.model().satelliteDomainIds().size() == 2
										&& fetched.model().hydratedSatellites().stream()
										          .map(SatelliteModel::value)
										          .toList()
										          .equals(List.of("one-updated", "three"));
							})
			).map(tc -> Arguments.of(tc.as(), tc));
		}

		private record BuilderCase(
				String as,
				java.util.function.Consumer<TestScenario> configure,
				java.util.function.Function<TestScenario, Boolean> assertion)
		{
			private static BuilderCase shape(
					final String as,
					final java.util.function.Consumer<TestScenario> configure,
					final java.util.function.Function<TestScenario, Boolean> assertion)
			{
				return new BuilderCase(as, configure, assertion);
			}
		}
	}
}