package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.equality.KeyBasedDuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.lifecycle.LifecycleSemantics;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.*;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.AbstractDeleteService;
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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Aggregate CRUD services satellite end-to-end tests")
final class AggregateCrudServicesSatelliteEndToEndTest
{
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

	private record MasterResponse(
			String value,
			List<Long> satelliteDomainIds,
			List<String> hydratedSatelliteValues)
	{
		private static MasterResponse from(final MasterModel masterDomainModel)
		{
			return new MasterResponse(
					masterDomainModel.value(),
					masterDomainModel.satelliteDomainIds(),
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
		private final TestMasterAggregateDefinition masterDefinition = new TestMasterAggregateDefinition();
		private final TestTransactionRunner transactionRunner = new TestTransactionRunner();
		private final DefaultAggregateLifecycleEngine engine = new DefaultAggregateLifecycleEngine(transactionRunner);
		private final TestSaveService saveService = new TestSaveService(masterDefinition, engine);
		private final TestUpdateService updateService = new TestUpdateService(masterDefinition, engine);
		private final TestFetchService fetchService = new TestFetchService(masterDefinition, engine);
		private final TestDeleteService deleteService = new TestDeleteService(masterDefinition, engine);
		private final Map<String, MasterModel> masterStore = new LinkedHashMap<>();
		private final Map<Long, SatelliteModel> satelliteStore = new LinkedHashMap<>();
		private final List<String> operationLog = new ArrayList<>();
		private final TestSatelliteAggregateDefinition satelliteDefinition = new TestSatelliteAggregateDefinition();

		private static TestScenario empty()
		{
			return new TestScenario();
		}

		private void installRelationship(
				final Cardinality cardinality,
				final ReconciliationStrategy reconciliationStrategy,
				final LifecycleSemantics lifecycleSemantics,
				final SatellitePersistenceOrder satellitePersistenceOrder)
		{
			masterDefinition.relationshipDefinitions = List.of(
					new TestRelationshipDefinition(cardinality, reconciliationStrategy, lifecycleSemantics,
							satellitePersistenceOrder));
		}

		private final class TestMasterAggregateDefinition
				implements AggregateCrudDefinition<String, MasterModel, MasterCreate, MasterPatch, MasterResponse>
		{
			private final AggregateMutationPort<String, MasterModel, MasterCreate, MasterPatch> mutationPort =
					new TestMasterMutationPort();
			private final AggregateFetchPort<String, MasterModel> fetchPort = new TestMasterFetchPort();
			private final AtomicInteger generatedIds = new AtomicInteger();
			private Collection<AggregateRelationshipDefinitionContract<String, MasterModel, MasterCreate, MasterPatch>>
					relationshipDefinitions = List.of();

			@Override
			public AggregateMutationPort<String, MasterModel, MasterCreate, MasterPatch> mutationPort()
			{
				return mutationPort;
			}

			@Override
			public AggregateFetchPort<String, MasterModel> fetchPort()
			{
				return fetchPort;
			}

			@Override
			public DomainModelBuilder<MasterCreate, MasterModel> createBuilder()
			{
				return create -> new MasterModel(create.value(), List.of(), List.of());
			}

			@Override
			public DomainModelPatcher<MasterModel, MasterPatch> patcher()
			{
				return (originalDomainModel, patch) -> new MasterModel(
						patch.value() == null ? originalDomainModel.value() : patch.value(),
						originalDomainModel.satelliteDomainIds(),
						originalDomainModel.hydratedSatellites());
			}

			@Override
			public DomainResponseBuilder<MasterModel, MasterResponse> responseBuilder()
			{
				return MasterResponse::from;
			}

			@Override
			public InsertionPolicy<MasterModel> insertionPolicy()
			{
				return _ ->
				{
				};
			}

			@Override
			public PatchPolicy<MasterModel> patchPolicy()
			{
				return (_, _) ->
				{
				};
			}

			@Override
			public DeletionPolicy<MasterModel> deletionPolicy()
			{
				return _ ->
				{
				};
			}

			@Override
			public DomainSecurityPolicy<MasterModel> securityPolicy()
			{
				return DomainSecurityPolicy.allowing();
			}

			@Override
			public DuplicateDefinition<MasterModel> duplicateDefinition()
			{
				return (KeyBasedDuplicateDefinition<MasterModel, String>) MasterModel::value;
			}

			@Override
			public Collection<AggregateRelationshipDefinitionContract<String, MasterModel, MasterCreate, MasterPatch>>
			relationshipDefinitions()
			{
				return relationshipDefinitions;
			}

			private final class TestMasterMutationPort
					implements AggregateMutationPort<String, MasterModel, MasterCreate, MasterPatch>
			{
				@Override
				public IdentifiedModel<String, MasterModel> create(final MasterModel domainModel)
				{
					var masterDomainId = "master-" + generatedIds.incrementAndGet();
					operationLog.add("master:create:" + domainModel.value());
					masterStore.put(masterDomainId, domainModel);
					return IdentifiedModel.of(masterDomainId, domainModel);
				}

				@Override
				public void put(final String masterDomainId, final MasterModel domainModel)
				{
					operationLog.add("master:put:" + masterDomainId);
					masterStore.put(masterDomainId, domainModel);
				}

				@Override
				public IdentifiedModel<String, MasterModel> update(final String masterDomainId,
				                                                   final MasterModel domainModel)
				{
					operationLog.add("master:update:" + masterDomainId);
					masterStore.put(masterDomainId, domainModel);
					return IdentifiedModel.of(masterDomainId, domainModel);
				}

				@Override
				public void delete(final String masterDomainId)
				{
					operationLog.add("master:delete:" + masterDomainId);
					masterStore.remove(masterDomainId);
				}
			}

			private final class TestMasterFetchPort implements AggregateFetchPort<String, MasterModel>
			{
				@Override
				public Optional<IdentifiedModel<String, MasterModel>> findById(final String masterDomainId)
				{
					return Optional.ofNullable(masterStore.get(masterDomainId))
					               .map(masterDomainModel -> IdentifiedModel.of(masterDomainId, masterDomainModel));
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
					                  .map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue()))
					                  .toList();
				}

				@Override
				public Slice<IdentifiedModel<String, MasterModel>> findAll(final Pageable pageable)
				{
					return new SliceImpl<>(findAll().stream().toList());
				}
			}
		}

		private final class TestSatelliteAggregateDefinition
				implements AggregateCrudDefinition<Long, SatelliteModel, SatelliteCreate, SatellitePatch, String>
		{
			private final AggregateMutationPort<Long, SatelliteModel, SatelliteCreate, SatellitePatch> mutationPort =
					new TestSatelliteMutationPort();
			private final AggregateFetchPort<Long, SatelliteModel> fetchPort = new TestSatelliteFetchPort();
			private final AtomicInteger generatedIds = new AtomicInteger();

			@Override
			public AggregateMutationPort<Long, SatelliteModel, SatelliteCreate, SatellitePatch> mutationPort()
			{
				return mutationPort;
			}

			@Override
			public AggregateFetchPort<Long, SatelliteModel> fetchPort()
			{
				return fetchPort;
			}

			@Override
			public DomainModelBuilder<SatelliteCreate, SatelliteModel> createBuilder()
			{
				return create -> new SatelliteModel(create.value());
			}

			@Override
			public DomainModelPatcher<SatelliteModel, SatellitePatch> patcher()
			{
				return (_, patch) -> new SatelliteModel(patch.value());
			}

			@Override
			public DomainResponseBuilder<SatelliteModel, String> responseBuilder()
			{
				return SatelliteModel::value;
			}

			@Override
			public InsertionPolicy<SatelliteModel> insertionPolicy()
			{
				return _ ->
				{
				};
			}

			@Override
			public PatchPolicy<SatelliteModel> patchPolicy()
			{
				return (_, _) ->
				{
				};
			}

			@Override
			public DeletionPolicy<SatelliteModel> deletionPolicy()
			{
				return _ ->
				{
				};
			}

			@Override
			public DomainSecurityPolicy<SatelliteModel> securityPolicy()
			{
				return DomainSecurityPolicy.allowing();
			}

			@Override
			public DuplicateDefinition<SatelliteModel> duplicateDefinition()
			{
				return (left, right) -> left.value().equals(right.value());
			}

			@Override
			public Collection<AggregateRelationshipDefinitionContract<Long, SatelliteModel, SatelliteCreate, SatellitePatch>>
			relationshipDefinitions()
			{
				return List.of();
			}

			private final class TestSatelliteMutationPort
					implements AggregateMutationPort<Long, SatelliteModel, SatelliteCreate, SatellitePatch>
			{
				@Override
				public IdentifiedModel<Long, SatelliteModel> create(final SatelliteModel domainModel)
				{
					generatedIds.updateAndGet(current -> Math.max(current, satelliteStore.keySet().stream()
					                                                                     .mapToInt(Long::intValue)
					                                                                     .max()
					                                                                     .orElse(0)));
					var satelliteDomainId = (long) generatedIds.incrementAndGet();
					operationLog.add("satellite:create:" + domainModel.value());
					satelliteStore.put(satelliteDomainId, domainModel);
					return IdentifiedModel.of(satelliteDomainId, domainModel);
				}

				@Override
				public void put(final Long satelliteDomainId, final SatelliteModel domainModel)
				{
					operationLog.add("satellite:put:" + satelliteDomainId);
					satelliteStore.put(satelliteDomainId, domainModel);
				}

				@Override
				public IdentifiedModel<Long, SatelliteModel> update(
						final Long satelliteDomainId,
						final SatelliteModel domainModel)
				{
					operationLog.add("satellite:update:" + satelliteDomainId);
					satelliteStore.put(satelliteDomainId, domainModel);
					return IdentifiedModel.of(satelliteDomainId, domainModel);
				}

				@Override
				public void delete(final Long satelliteDomainId)
				{
					operationLog.add("satellite:delete:" + satelliteDomainId);
					satelliteStore.remove(satelliteDomainId);
				}
			}

			private final class TestSatelliteFetchPort implements AggregateFetchPort<Long, SatelliteModel>
			{
				@Override
				public Optional<IdentifiedModel<Long, SatelliteModel>> findById(final Long satelliteDomainId)
				{
					return Optional.ofNullable(satelliteStore.get(satelliteDomainId))
					               .map(satelliteDomainModel -> IdentifiedModel.of(satelliteDomainId,
										   satelliteDomainModel));
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
					                     .map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue()))
					                     .toList();
				}

				@Override
				public Slice<IdentifiedModel<Long, SatelliteModel>> findAll(final Pageable pageable)
				{
					return new SliceImpl<>(findAll().stream().toList());
				}
			}
		}

		private final class TestRelationshipDefinition
				implements AggregateRelationshipDefinition<String, MasterModel, MasterCreate, MasterPatch, Long,
				SatelliteModel, SatelliteCreate, SatellitePatch>
		{
			private final Cardinality cardinality;
			private final ReconciliationStrategy reconciliationStrategy;
			private final LifecycleSemantics lifecycleSemantics;
			private final SatellitePersistenceOrder satellitePersistenceOrder;

			@Override
			public String name()
			{
				return "satellite";
			}

			@Override
			public Cardinality cardinality()
			{
				return cardinality;
			}

			@Override
			public LifecycleSemantics lifecycleSemantics()
			{
				return lifecycleSemantics;
			}

			@Override
			public ReconciliationStrategy reconciliationStrategy()
			{
				return reconciliationStrategy;
			}

			@Override
			public AggregateCrudDefinition<Long, SatelliteModel, SatelliteCreate, SatellitePatch, ?> satelliteDefinition()
			{
				return satelliteDefinition;
			}

			@Override
			public AggregateMutationPort<Long, SatelliteModel, SatelliteCreate, SatellitePatch> satelliteMutationPort()
			{
				return satelliteDefinition.mutationPort();
			}

			@Override
			public AggregateFetchPort<Long, SatelliteModel> satelliteFetchPort()
			{
				return satelliteDefinition.fetchPort();
			}

			@Override
			public SatelliteCreateInputResolver<MasterCreate, Collection<SatelliteCreateIntent<Long, SatelliteCreate>>>
			createInputResolver()
			{
				return MasterCreate::satelliteCreateIntents;
			}

			@Override
			public SatellitePatchInputResolver<MasterPatch,
					Collection<SatelliteMutationIntent<Long, SatelliteCreate, SatellitePatch>>> patchInputResolver()
			{
				return MasterPatch::satelliteMutationIntents;
			}

			@Override
			public SatelliteIdentityResolver<MasterModel, SatelliteModel, Long> identityResolver()
			{
				return (_, _) -> Optional.empty();
			}

			@Override
			public SatelliteLinkStrategy<String, MasterModel, Long, SatelliteModel> linkStrategy()
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

			@Override
			public SatelliteHydrationStrategy<String, MasterModel, Long, SatelliteModel> hydrationStrategy()
			{
				return (master, satelliteFetchPort, satelliteLinkStrategy) ->
				{
					var hydratedSatellites = new ArrayList<IdentifiedModel<Long, SatelliteModel>>();
					for (var satelliteDomainId : satelliteLinkStrategy.currentLinkedSatelliteDomainIds(master.model()))
					{
						satelliteFetchPort.findById(satelliteDomainId).ifPresent(hydratedSatellites::add);
					}
					return satelliteLinkStrategy.attachHydratedSatellites(master.model(), hydratedSatellites);
				};
			}

			private TestRelationshipDefinition(
					final Cardinality cardinality,
					final ReconciliationStrategy reconciliationStrategy,
					final LifecycleSemantics lifecycleSemantics,
					final SatellitePersistenceOrder satellitePersistenceOrder)
			{
				this.cardinality = cardinality;
				this.reconciliationStrategy = reconciliationStrategy;
				this.lifecycleSemantics = lifecycleSemantics;
				this.satellitePersistenceOrder = satellitePersistenceOrder;
			}
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

	private static final class TestDeleteService
			extends AbstractDeleteService<String, MasterModel, MasterCreate, MasterPatch, MasterResponse>
	{
		private TestDeleteService(
				final AggregateCrudDefinition<String, MasterModel, MasterCreate, MasterPatch, MasterResponse> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine);
		}
	}

	private static final class TestTransactionRunner implements PersistenceTransactionRunner
	{
		@Override
		public <T> T inTransaction(final Supplier<T> action)
		{
			return action.get();
		}
	}

	@Nested
	@DisplayName("Save and fetch")
	final class SaveAndFetchTests
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("saveCases")
		@DisplayName("save() should round-trip a one-to-one satellite through the real services")
		void saveShouldRoundTripOneToOneSatelliteThroughTheRealServices(final String as, final SaveCase tc)
		{
			var scenario = TestScenario.empty();
			scenario.installRelationship(
					Cardinality.ONE,
					ReconciliationStrategy.REPLACE,
					LifecycleSemantics.of(true, true, true, true, true),
					tc.persistenceOrder());

			var saved = scenario.saveService.save(new MasterCreate(
					"master",
					List.of(new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
							new SatelliteCreate("satellite")))));
			var fetched = scenario.fetchService.findById(saved.id());

			assertThat(saved.model().value())
					.as("saved response value should match for %s", as)
					.isEqualTo("master");
			assertThat(saved.model().satelliteDomainIds())
					.as("saved response should expose linked satellite IDs for %s", as)
					.containsExactly(1L);
			assertThat(fetched.model().hydratedSatellites())
					.as("fetch should hydrate linked satellite for %s", as)
					.extracting(SatelliteModel::value)
					.containsExactly("satellite");
			assertThat(scenario.operationLog)
					.as("operation order should match for %s", as)
					.startsWith(tc.expectedOperationPrefix().toArray(String[]::new));
		}

		private static Stream<Arguments> saveCases()
		{
			return Stream.of(
					SaveCase.shape(
							"satellite persisted before master",
							SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER,
							List.of("satellite:create:satellite", "master:create:master")),
					SaveCase.shape(
							"master persisted before satellite",
							SatellitePersistenceOrder.MASTER_BEFORE_SATELLITE,
							List.of("master:create:master", "satellite:create:satellite", "master:update:master-1"))
			).map(tc -> Arguments.of(tc.as(), tc));
		}

		private record SaveCase(
				String as,
				SatellitePersistenceOrder persistenceOrder,
				List<String> expectedOperationPrefix)
		{
			private static SaveCase shape(
					final String as,
					final SatellitePersistenceOrder persistenceOrder,
					final List<String> expectedOperationPrefix)
			{
				return new SaveCase(as, persistenceOrder, expectedOperationPrefix);
			}
		}
	}

	@Nested
	@DisplayName("Update and fetch")
	final class UpdateAndFetchTests
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("updateCases")
		@DisplayName("updateById() should preserve orchestrated satellite behavior through the real services")
		void updateByIdShouldPreserveOrchestratedSatelliteBehaviorThroughTheRealServices(
				final String as,
				final UpdateCase tc)
		{
			var scenario = TestScenario.empty();
			scenario.installRelationship(tc.cardinality(), tc.reconciliationStrategy(), tc.lifecycleSemantics(),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
			tc.seed().accept(scenario);

			var updated = scenario.updateService.updateById("master-1", tc.patch());
			var fetched = scenario.fetchService.findById("master-1");

			assertThat(updated.model().satelliteDomainIds())
					.as("updated response should expose linked satellite IDs for %s", as)
					.hasSize(tc.expectedLinkedSatelliteCount());
			assertThat(fetched.model().hydratedSatellites())
					.as("fetched master should reflect the updated hydrated satellites for %s", as)
					.extracting(SatelliteModel::value)
					.containsExactlyElementsOf(tc.expectedHydratedSatelliteValues());
		}

		private static Stream<Arguments> updateCases()
		{
			return Stream.of(
					UpdateCase.shape(
							"one-to-one create replacement with orphan delete",
							Cardinality.ONE,
							ReconciliationStrategy.REPLACE,
							LifecycleSemantics.of(true, true, false, true, true),
							scenario ->
							{
								scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L), List.of()));
								scenario.satelliteStore.put(1L, new SatelliteModel("old"));
							},
							new MasterPatch(
									null,
									List.of(new SatelliteMutationIntent.CreateSatelliteMutationIntent<>(
											new SatelliteCreate(
													"new")))),
							1,
							List.of("new")),
					UpdateCase.shape(
							"many merge-by-id mixed update and create",
							Cardinality.MANY,
							ReconciliationStrategy.MERGE_BY_ID,
							LifecycleSemantics.of(true, true, true, true, true),
							scenario ->
							{
								scenario.masterStore.put("master-1",
										new MasterModel("master", List.of(1L, 2L), List.of()));
								scenario.satelliteStore.put(1L, new SatelliteModel("one"));
								scenario.satelliteStore.put(2L, new SatelliteModel("two"));
							},
							new MasterPatch(
									null,
									List.of(
											new SatelliteMutationIntent.UpdateSatelliteMutationIntent<>(1L,
													new SatellitePatch("one-updated")),
											new SatelliteMutationIntent.RemoveSatelliteMutationIntent<>(2L),
											new SatelliteMutationIntent.CreateSatelliteMutationIntent<>(
													new SatelliteCreate("three")))),
							2,
							List.of("one-updated", "three")),
					UpdateCase.shape(
							"many replace unlinks omitted satellites when orphan delete is disabled",
							Cardinality.MANY,
							ReconciliationStrategy.REPLACE,
							LifecycleSemantics.of(true, true, false, false, true),
							scenario ->
							{
								scenario.masterStore.put("master-1",
										new MasterModel("master", List.of(1L, 2L), List.of()));
								scenario.satelliteStore.put(1L, new SatelliteModel("one"));
								scenario.satelliteStore.put(2L, new SatelliteModel("two"));
							},
							new MasterPatch(
									null,
									List.of(new SatelliteMutationIntent.ReferenceSatelliteMutationIntent<>(1L))),
							1,
							List.of("one"))
			).map(tc -> Arguments.of(tc.as(), tc));
		}

		private record UpdateCase(
				String as,
				Cardinality cardinality,
				ReconciliationStrategy reconciliationStrategy,
				LifecycleSemantics lifecycleSemantics,
				Consumer<TestScenario> seed,
				MasterPatch patch,
				int expectedLinkedSatelliteCount,
				List<String> expectedHydratedSatelliteValues)
		{
			private static UpdateCase shape(
					final String as,
					final Cardinality cardinality,
					final ReconciliationStrategy reconciliationStrategy,
					final LifecycleSemantics lifecycleSemantics,
					final Consumer<TestScenario> seed,
					final MasterPatch patch,
					final int expectedLinkedSatelliteCount,
					final List<String> expectedHydratedSatelliteValues)
			{
				return new UpdateCase(as, cardinality, reconciliationStrategy, lifecycleSemantics, seed, patch,
						expectedLinkedSatelliteCount, expectedHydratedSatelliteValues);
			}
		}
	}

	@Nested
	@DisplayName("Delete")
	final class DeleteTests
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("deleteCases")
		@DisplayName("deleteById() should respect cascade delete through the real services")
		void deleteByIdShouldRespectCascadeDeleteThroughTheRealServices(final String as, final DeleteCase tc)
		{
			var scenario = TestScenario.empty();
			scenario.installRelationship(
					Cardinality.ONE,
					ReconciliationStrategy.REPLACE,
					tc.lifecycleSemantics(),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
			scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L), List.of()));
			scenario.satelliteStore.put(1L, new SatelliteModel("satellite"));

			scenario.deleteService.deleteById("master-1");

			assertThat(scenario.masterStore)
					.as("master should be deleted for %s", as)
					.doesNotContainKey("master-1");
			assertThat(scenario.satelliteStore.containsKey(1L))
					.as("satellite presence should match cascade semantics for %s", as)
					.isEqualTo(tc.expectedSatelliteStillPresent());
		}

		private static Stream<Arguments> deleteCases()
		{
			return Stream.of(
					DeleteCase.shape("cascade delete enabled", LifecycleSemantics.of(false, false, true, false, false),
							false),
					DeleteCase.shape("cascade delete disabled", LifecycleSemantics.none(), true)
			).map(tc -> Arguments.of(tc.as(), tc));
		}

		private record DeleteCase(String as, LifecycleSemantics lifecycleSemantics,
		                          boolean expectedSatelliteStillPresent)
		{
			private static DeleteCase shape(
					final String as,
					final LifecycleSemantics lifecycleSemantics,
					final boolean expectedSatelliteStillPresent)
			{
				return new DeleteCase(as, lifecycleSemantics, expectedSatelliteStillPresent);
			}
		}
	}
}