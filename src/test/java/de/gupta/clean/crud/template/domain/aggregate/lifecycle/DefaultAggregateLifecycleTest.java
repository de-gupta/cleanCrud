package de.gupta.clean.crud.template.domain.aggregate.lifecycle;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutation;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.domain.aggregate.execution.*;
import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.domain.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.domain.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.domain.aggregate.relationship.*;
import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceCannotBeDeletedException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
import de.gupta.clean.crud.template.domain.relationship.RelationshipKind;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.equality.KeyBasedDuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAggregateLifecycleTest
{
	@Test
	void zeroRelationshipCrudStillWorks()
	{
		var scenario = new TestScenario(List.of());

		var saved = scenario.engine.save(scenario.masterDefinition, new MasterCreate("alpha", List.of()));

		assertEquals(1, scenario.transactionRunner.transactionCount());
		assertEquals("master-1", saved.id());
		assertEquals("alpha", saved.model().value());
	}

	@Test
	void oneToOneSaveFetchDeleteLifecycleWorks()
	{
		var scenario = new TestScenario(List.of());
		scenario.installRelationship(
				Cardinality.ONE,
				ReconciliationStrategy.REPLACE,
				LifecycleSemantics.of(true, true, true, true, true),
				SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);

		var saved = scenario.engine.save(
				scenario.masterDefinition,
				new MasterCreate(
						"master",
						List.of(new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(new SatelliteCreate("sat")))));
		assertEquals(List.of(1L), saved.model().satelliteDomainIds());
		assertEquals(List.of("satellite:create:sat", "master:create:master"), scenario.operationLog.subList(0, 2));

		var fetched = scenario.engine.findById(scenario.masterDefinition, saved.id());
		assertEquals(1, fetched.model().hydratedSatellites().size());
		assertEquals("sat", fetched.model().hydratedSatellites().getFirst().value());

		scenario.engine.deleteById(scenario.masterDefinition, saved.id());
		assertFalse(scenario.masterStore.containsKey(saved.id()));
		assertFalse(scenario.satelliteStore.containsKey(1L));
	}

	@Test
	void oneToOneSaveSupportsReferenceSatelliteCreateIntent()
	{
		var scenario = new TestScenario(List.of());
		scenario.installRelationship(
				Cardinality.ONE,
				ReconciliationStrategy.REPLACE,
				LifecycleSemantics.of(true, true, true, true, true),
				SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
		scenario.satelliteStore.put(7L, new SatelliteModel("existing"));

		var saved = scenario.engine.save(
				scenario.masterDefinition,
				new MasterCreate(
						"master",
						List.of(new SatelliteCreateIntent.ReferenceSatelliteCreateIntent<>(7L))));

		assertEquals(List.of(7L), saved.model().satelliteDomainIds());
		assertEquals("existing", scenario.satelliteStore.get(7L).value());
	}

	@Test
	void saveAllWithRelationshipsRejectsDuplicateRequestItemsBeforePersistence()
	{
		var scenario = new TestScenario(List.of());
		scenario.installRelationship(
				Cardinality.ONE,
				ReconciliationStrategy.REPLACE,
				LifecycleSemantics.of(true, true, false, false, false),
				SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);

		assertThrows(
				InvalidRequestException.class,
				() -> scenario.engine.saveAll(
						scenario.masterDefinition,
						List.of(
								new MasterCreate(
										"duplicate",
										List.of(new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
												new SatelliteCreate("sat-one")))),
								new MasterCreate(
										"duplicate",
										List.of(new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
												new SatelliteCreate("sat-two")))))));
		assertTrue(scenario.masterStore.isEmpty());
		assertTrue(scenario.satelliteStore.isEmpty());
	}

	@Test
	void oneToOneUpdateSupportsReferenceCreateUpdateAndRemove()
	{
		var scenario = new TestScenario(List.of());
		scenario.installRelationship(
				Cardinality.ONE,
				ReconciliationStrategy.REPLACE,
				LifecycleSemantics.of(true, true, false, true, false),
				SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
		scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L), List.of()));
		scenario.satelliteStore.put(1L, new SatelliteModel("old"));

		scenario.engine.updateById(
				scenario.masterDefinition,
				"master-1",
				new MasterPatch(
						null,
						List.of(new SatelliteMutationIntent.UpdateSatelliteMutationIntent<>(1L,
								new SatellitePatch("updated")))));
		assertEquals("updated", scenario.satelliteStore.get(1L).value());

		scenario.satelliteStore.put(2L, new SatelliteModel("other"));
		var referenced = scenario.engine.updateById(
				scenario.masterDefinition,
				"master-1",
				new MasterPatch(null, List.of(new SatelliteMutationIntent.ReferenceSatelliteMutationIntent<>(2L))));
		assertEquals(List.of(2L), referenced.model().satelliteDomainIds());
		assertFalse(scenario.satelliteStore.containsKey(1L));

		var created = scenario.engine.updateById(
				scenario.masterDefinition,
				"master-1",
				new MasterPatch(
						null,
						List.of(new SatelliteMutationIntent.CreateSatelliteMutationIntent<>(
								new SatelliteCreate("new")))));
		var createdSatelliteDomainId = created.model().satelliteDomainIds().getFirst();
		assertEquals("new", scenario.satelliteStore.get(createdSatelliteDomainId).value());

		var removed = scenario.engine.updateById(
				scenario.masterDefinition,
				"master-1",
				new MasterPatch(
						null,
						List.of(new SatelliteMutationIntent.RemoveSatelliteMutationIntent<>(
								createdSatelliteDomainId))));
		assertTrue(removed.model().satelliteDomainIds().isEmpty());
		assertFalse(scenario.satelliteStore.containsKey(createdSatelliteDomainId));
	}

	@Test
	void oneToOneUpdateCurrentSatelliteUpdatesTheSingleCurrentlyLinkedSatellite()
	{
		var scenario = new TestScenario(List.of());
		scenario.installRelationship(
				Cardinality.ONE,
				ReconciliationStrategy.REPLACE,
				LifecycleSemantics.of(true, true, false, true, false),
				SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
		scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L), List.of()));
		scenario.satelliteStore.put(1L, new SatelliteModel("old"));

		var updated = scenario.engine.updateById(
				scenario.masterDefinition,
				"master-1",
				new MasterPatch(
						null,
						List.of(new SatelliteMutationIntent.UpdateCurrentSatelliteMutationIntent<>(
								new SatellitePatch("updated")))));

		assertEquals(List.of(1L), updated.model().satelliteDomainIds());
		assertEquals("updated", scenario.satelliteStore.get(1L).value());
	}

	@Test
	void manyReplaceAndMergeByIdAreBothSupported()
	{
		var replaceScenario = new TestScenario(List.of());
		replaceScenario.installRelationship(
				Cardinality.MANY,
				ReconciliationStrategy.REPLACE,
				LifecycleSemantics.of(true, true, true, true, true),
				SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
		replaceScenario.masterStore.put("master-1", new MasterModel("master", List.of(1L, 2L), List.of()));
		replaceScenario.satelliteStore.put(1L, new SatelliteModel("one"));
		replaceScenario.satelliteStore.put(2L, new SatelliteModel("two"));

		var replaced = replaceScenario.engine.updateById(
				replaceScenario.masterDefinition,
				"master-1",
				new MasterPatch(
						null,
						List.of(
								new SatelliteMutationIntent.UpdateSatelliteMutationIntent<>(1L,
										new SatellitePatch("one-updated")),
								new SatelliteMutationIntent.CreateSatelliteMutationIntent<>(
										new SatelliteCreate("three")))));
		assertEquals("one-updated", replaceScenario.satelliteStore.get(1L).value());
		assertFalse(replaceScenario.satelliteStore.containsKey(2L));
		assertEquals(2, replaced.model().satelliteDomainIds().size());

		var mergeScenario = new TestScenario(List.of());
		mergeScenario.installRelationship(
				Cardinality.MANY,
				ReconciliationStrategy.MERGE_BY_ID,
				LifecycleSemantics.of(true, true, true, true, true),
				SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
		mergeScenario.masterStore.put("master-1", new MasterModel("master", List.of(1L, 2L), List.of()));
		mergeScenario.satelliteStore.put(1L, new SatelliteModel("one"));
		mergeScenario.satelliteStore.put(2L, new SatelliteModel("two"));

		var merged = mergeScenario.engine.updateById(
				mergeScenario.masterDefinition,
				"master-1",
				new MasterPatch(
						null,
						List.of(
								new SatelliteMutationIntent.UpdateSatelliteMutationIntent<>(1L,
										new SatellitePatch("one-updated")),
								new SatelliteMutationIntent.RemoveSatelliteMutationIntent<>(2L),
								new SatelliteMutationIntent.CreateSatelliteMutationIntent<>(
										new SatelliteCreate("three")))));
		assertTrue(merged.model().satelliteDomainIds().contains(1L));
		assertFalse(mergeScenario.satelliteStore.containsKey(2L));
		assertEquals(2, merged.model().satelliteDomainIds().size());
	}

	@Test
	void oneRelationshipDoesNotSupportMergeById()
	{
		var scenario = new TestScenario(List.of());
		scenario.installRelationship(
				Cardinality.ONE,
				ReconciliationStrategy.MERGE_BY_ID,
				LifecycleSemantics.of(true, true, false, false, false),
				SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);

		assertThrows(
				AggregateRelationshipExecutionNotSupportedException.class,
				() -> scenario.engine.save(
						scenario.masterDefinition,
						new MasterCreate(
								"master",
								List.of(new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
										new SatelliteCreate("satellite"))))));
	}

	@Test
	void manyReplaceWithoutOrphanDeleteUnlinksRemovedSatellites()
	{
		var scenario = new TestScenario(List.of());
		scenario.installRelationship(
				Cardinality.MANY,
				ReconciliationStrategy.REPLACE,
				LifecycleSemantics.of(true, true, false, false, true),
				SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
		scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L, 2L), List.of()));
		scenario.satelliteStore.put(1L, new SatelliteModel("one"));
		scenario.satelliteStore.put(2L, new SatelliteModel("two"));

		var updated = scenario.engine.updateById(
				scenario.masterDefinition,
				"master-1",
				new MasterPatch(null, List.of(
						new SatelliteMutationIntent.ReferenceSatelliteMutationIntent<>(1L))));

		assertEquals(List.of(1L), updated.model().satelliteDomainIds());
		assertTrue(scenario.satelliteStore.containsKey(2L));
	}

	@Test
	void manyRelationshipsRejectImplicitCurrentSatelliteMutationIntents()
	{
		for (var reconciliationStrategy : List.of(ReconciliationStrategy.REPLACE, ReconciliationStrategy.MERGE_BY_ID))
		{
			var scenario = new TestScenario(List.of());
			scenario.installRelationship(
					Cardinality.MANY,
					reconciliationStrategy,
					LifecycleSemantics.of(true, true, false, false, false),
					SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
			scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L, 2L), List.of()));
			scenario.satelliteStore.put(1L, new SatelliteModel("one"));
			scenario.satelliteStore.put(2L, new SatelliteModel("two"));

			List<SatelliteMutationIntent<Long, SatelliteCreate, SatellitePatch>> mutationIntents = List.of(
					new SatelliteMutationIntent.UpsertCurrentSatelliteMutationIntent<>(
							new SatelliteCreate("created"),
							new SatellitePatch("patched")),
					new SatelliteMutationIntent.UpdateCurrentSatelliteMutationIntent<>(
							new SatellitePatch("patched")),
					new SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent<>());
			for (var mutationIntent : mutationIntents)
			{
				var exception = assertThrows(
						InvalidRequestException.class,
						() -> scenario.engine.updateById(
								scenario.masterDefinition,
								"master-1",
								new MasterPatch(null, List.of(mutationIntent))));
				assertEquals(
						"Relationship 'satellite' cannot use implicit current satellite mutations for MANY cardinality; use explicit satellite ids instead",
						exception.getMessage());
			}

			assertEquals(List.of(1L, 2L), scenario.masterStore.get("master-1").satelliteDomainIds());
			assertTrue(scenario.satelliteStore.containsKey(1L));
			assertTrue(scenario.satelliteStore.containsKey(2L));
		}
	}

	@Test
	void replaceRemoveCurrentRemovesTheSingleCurrentlyLinkedSatellite()
	{
		var scenario = new TestScenario(List.of());
		scenario.installRelationship(
				Cardinality.ONE,
				ReconciliationStrategy.REPLACE,
				LifecycleSemantics.of(true, true, false, true, false),
				SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
		scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L), List.of()));
		scenario.satelliteStore.put(1L, new SatelliteModel("one"));

		var updated = scenario.engine.updateById(
				scenario.masterDefinition,
				"master-1",
				new MasterPatch(
						null,
						List.of(new SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent<>())));

		assertTrue(updated.model().satelliteDomainIds().isEmpty());
		assertFalse(scenario.satelliteStore.containsKey(1L));
	}

	@Test
	void replaceRemoveCurrentRequiresOrphanDeleteToBeEnabled()
	{
		var scenario = new TestScenario(List.of());
		scenario.installRelationship(
				Cardinality.ONE,
				ReconciliationStrategy.REPLACE,
				LifecycleSemantics.of(true, true, false, false, false),
				SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
		scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L), List.of()));
		scenario.satelliteStore.put(1L, new SatelliteModel("one"));

		var exception = assertThrows(
				InvalidRequestException.class,
				() -> scenario.engine.updateById(
						scenario.masterDefinition,
						"master-1",
						new MasterPatch(
								null,
								List.of(new SatelliteMutationIntent.RemoveCurrentSatelliteMutationIntent<>()))));

		assertEquals(
				"Relationship 'satellite' cannot remove the current satellite under REPLACE unless orphanDelete is enabled",
				exception.getMessage());
		assertEquals(List.of(1L), scenario.masterStore.get("master-1").satelliteDomainIds());
		assertTrue(scenario.satelliteStore.containsKey(1L));
	}

	@Test
	void updateAndRemoveRequireCurrentlyLinkedSatelliteDomainIds()
	{
		var scenario = new TestScenario(List.of());
		scenario.installRelationship(
				Cardinality.MANY,
				ReconciliationStrategy.MERGE_BY_ID,
				LifecycleSemantics.of(true, true, false, false, false),
				SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER);
		scenario.masterStore.put("master-1", new MasterModel("master", List.of(1L), List.of()));
		scenario.satelliteStore.put(1L, new SatelliteModel("one"));
		scenario.satelliteStore.put(2L, new SatelliteModel("two"));

		assertThrows(
				InvalidRequestException.class,
				() -> scenario.engine.updateById(
						scenario.masterDefinition,
						"master-1",
						new MasterPatch(
								null,
								List.of(new SatelliteMutationIntent.UpdateSatelliteMutationIntent<>(2L,
										new SatellitePatch("updated"))))));

		assertThrows(
				InvalidRequestException.class,
				() -> scenario.engine.updateById(
						scenario.masterDefinition,
						"master-1",
						new MasterPatch(
								null,
								List.of(new SatelliteMutationIntent.RemoveSatelliteMutationIntent<>(2L)))));
	}

	@Test
	void bestEffortStillUsesPerItemTransactions()
	{
		var scenario = new TestScenario(List.of());
		scenario.masterStore.put("ok", new MasterModel("ok", List.of(), List.of()));
		scenario.masterStore.put("blocked", new MasterModel("blocked", List.of(), List.of()));
		scenario.masterDefinition.deletionPolicy = model ->
		{
			if ("blocked".equals(model.value()))
			{
				throw ResourceCannotBeDeletedException.withMessage("blocked");
			}
		};

		scenario.engine.deleteAllById(scenario.masterDefinition, List.of("ok", "blocked"),
				BulkOperationMode.BEST_EFFORT);

		assertEquals(2, scenario.transactionRunner.transactionCount());
		assertFalse(scenario.masterStore.containsKey("ok"));
		assertTrue(scenario.masterStore.containsKey("blocked"));
	}

	@Test
	void updateAllByIdUpdatesAllRequestedModels()
	{
		var scenario = new TestScenario(List.of());
		scenario.masterStore.put("first", new MasterModel("first", List.of(), List.of()));
		scenario.masterStore.put("second", new MasterModel("second", List.of(), List.of()));

		var updated = scenario.engine.updateAllById(
				scenario.masterDefinition,
				List.of(
						IdentifiedModel.of("first", new MasterPatch("first-updated", List.of())),
						IdentifiedModel.of("second", new MasterPatch("second-updated", List.of()))),
				BulkOperationMode.ALL_OR_NOTHING);

		assertEquals(2, updated.size());
		assertEquals("first-updated", scenario.masterStore.get("first").value());
		assertEquals("second-updated", scenario.masterStore.get("second").value());
		assertEquals(1, scenario.transactionRunner.transactionCount());
	}

	@Test
	void postCommitMutationContextsAreProducedForAllMutationKinds()
	{
		var dispatcher = new RecordingDispatcher();
		var scenario = new TestScenario(List.of(), dispatcher);
		scenario.masterDefinition.postCommitMutation = dispatcher.contexts::add;

		var created = scenario.engine.save(scenario.masterDefinition, new MasterCreate("created", List.of()));
		scenario.engine.putAtId(scenario.masterDefinition, created.id(), new MasterCreate("put", List.of()));
		var patched = scenario.engine.updateById(
				scenario.masterDefinition,
				created.id(),
				new MasterPatch("patched", List.of()));
		scenario.engine.deleteById(scenario.masterDefinition, created.id());

		assertEquals(
				List.of(
						new PostCommitMutationContext<>(PostCommitMutationKind.CREATE, created.id(),
								Optional.of(created.model()), Optional.empty()),
						new PostCommitMutationContext<>(PostCommitMutationKind.PUT, created.id(),
								Optional.of(new MasterModel("put", List.of(), List.of())),
								Optional.of(new MasterModel("created", List.of(), List.of()))),
						new PostCommitMutationContext<>(PostCommitMutationKind.PATCH, patched.id(),
								Optional.of(patched.model()),
								Optional.of(new MasterModel("put", List.of(), List.of()))),
						new PostCommitMutationContext<>(PostCommitMutationKind.DELETE, created.id(),
								Optional.empty(),
								Optional.of(new MasterModel("patched", List.of(), List.of())))),
				dispatcher.contexts);
	}

	@Test
	void postCommitMutationIsNotDispatchedWhenMutationFailsBeforeCommit()
	{
		var dispatcher = new RecordingDispatcher();
		var scenario = new TestScenario(List.of(), dispatcher);
		scenario.masterDefinition.deletionPolicy = model ->
		{
			throw ResourceCannotBeDeletedException.withMessage(model.value());
		};
		scenario.masterStore.put("blocked", new MasterModel("blocked", List.of(), List.of()));

		assertThrows(ResourceCannotBeDeletedException.class,
				() -> scenario.engine.deleteById(scenario.masterDefinition, "blocked"));
		assertTrue(dispatcher.contexts.isEmpty());
	}

	@Test
	void postCommitMutationFailureDoesNotChangeCommittedMutationResult()
	{
		var dispatcher = new SwallowingRecordingDispatcher();
		var scenario = new TestScenario(List.of(), dispatcher);
		scenario.masterDefinition.postCommitMutation = _ ->
		{
			throw new RuntimeException("hook failed");
		};

		var saved = scenario.engine.save(scenario.masterDefinition, new MasterCreate("created", List.of()));

		assertEquals("master-1", saved.id());
		assertEquals("created", scenario.masterStore.get(saved.id()).value());
	}

	private record MasterCreate(String value,
	                            Collection<SatelliteCreateIntent<Long, SatelliteCreate>> satelliteCreateIntents)
	{
	}

	private record MasterPatch(String value,
	                           Collection<SatelliteMutationIntent<Long, SatelliteCreate, SatellitePatch>> satelliteMutationIntents)
	{
	}

	private record MasterModel(String value, List<Long> satelliteDomainIds, List<SatelliteModel> hydratedSatellites)
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
		private final TestTransactionRunner transactionRunner = new TestTransactionRunner();
		private final AggregateLifecycle workflowEngine;
		private final TestEngineFacade engine;
		private final Map<String, MasterModel> masterStore = new LinkedHashMap<>();
		private final Map<Long, SatelliteModel> satelliteStore = new LinkedHashMap<>();
		private final List<String> operationLog = new ArrayList<>();
		private final TestMasterAggregateDefinition masterDefinition;
		private final TestSatelliteAggregateDefinition satelliteDefinition;

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

		private TestScenario(
				final List<AggregateRelationshipDefinitionContract<String, MasterModel, MasterCreate, MasterPatch>> relationshipDefinitions)
		{
			this(relationshipDefinitions, PostCommitMutationDispatcher.async());
		}

		private TestScenario(
				final List<AggregateRelationshipDefinitionContract<String, MasterModel, MasterCreate, MasterPatch>> relationshipDefinitions,
				final PostCommitMutationDispatcher postCommitMutationDispatcher)
		{
			this.satelliteDefinition = new TestSatelliteAggregateDefinition();
			this.masterDefinition = new TestMasterAggregateDefinition(relationshipDefinitions);
			this.workflowEngine = DefaultAggregateLifecycle.withTransactionRunnerAndDispatcher(transactionRunner,
					postCommitMutationDispatcher);
			this.engine = new TestEngineFacade(workflowEngine);
		}

		private final class TestMasterAggregateDefinition
				implements AggregateDefinition<String, MasterModel, MasterCreate, MasterPatch, String>
		{
			private final AggregateMutationPort<String, MasterModel, MasterCreate, MasterPatch> mutationPort =
					new TestMasterMutationPort();
			private final AggregateFetchPort<String, MasterModel> fetchPort = new TestMasterFetchPort();
			private final AtomicInteger generatedIds = new AtomicInteger();
			private Collection<AggregateRelationshipDefinitionContract<String, MasterModel, MasterCreate, MasterPatch>>
					relationshipDefinitions;
			private DeletionPolicy<MasterModel> deletionPolicy = _ ->
			{
			};
			private PostCommitMutation<String, MasterModel> postCommitMutation = PostCommitMutation.noop();

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
			public DomainResponseBuilder<MasterModel, String> responseBuilder()
			{
				return MasterModel::value;
			}

			@Override
			public DeletionPolicy<MasterModel> deletionPolicy()
			{
				return deletionPolicy;
			}

			@Override
			public DuplicateDefinition<MasterModel> duplicateDefinition()
			{
				return (KeyBasedDuplicateDefinition<MasterModel, String>) MasterModel::value;
			}

			@Override
			public PostCommitMutation<String, MasterModel> postCommitMutation()
			{
				return postCommitMutation;
			}

			@Override
			public Collection<AggregateRelationshipDefinitionContract<String, MasterModel, MasterCreate, MasterPatch>>
			relationshipDefinitions()
			{
				return relationshipDefinitions;
			}

			@Override
			public DomainSecurityPolicy<MasterModel> securityPolicy()
			{
				return DomainSecurityPolicy.allowing();
			}

			@Override
			public PatchPolicy<MasterModel> patchPolicy()
			{
				return (_, _) ->
				{
				};
			}

			@Override
			public InsertionPolicy<MasterModel> insertionPolicy()
			{
				return _ ->
				{
				};
			}

			private TestMasterAggregateDefinition(
					final Collection<AggregateRelationshipDefinitionContract<String, MasterModel, MasterCreate, MasterPatch>>
							relationshipDefinitions)
			{
				this.relationshipDefinitions = relationshipDefinitions;
			}

			private final class TestMasterMutationPort
					implements AggregateMutationPort<String, MasterModel, MasterCreate, MasterPatch>
			{
				@Override
				public IdentifiedModel<String, MasterModel> create(final MasterModel domainModel)
				{
					var id = "master-" + generatedIds.incrementAndGet();
					operationLog.add("master:create:" + domainModel.value());
					masterStore.put(id, domainModel);
					return IdentifiedModel.of(id, domainModel);
				}

				@Override
				public void put(final String domainId, final MasterModel domainModel)
				{
					operationLog.add("master:put:" + domainId);
					masterStore.put(domainId, domainModel);
				}

				@Override
				public IdentifiedModel<String, MasterModel> update(final String domainId, final MasterModel domainModel)
				{
					operationLog.add("master:update:" + domainId);
					masterStore.put(domainId, domainModel);
					return IdentifiedModel.of(domainId, domainModel);
				}

				@Override
				public void delete(final String domainId)
				{
					operationLog.add("master:delete:" + domainId);
					masterStore.remove(domainId);
				}
			}

			private final class TestMasterFetchPort implements AggregateFetchPort<String, MasterModel>
			{
				@Override
				public Optional<IdentifiedModel<String, MasterModel>> findById(final String domainId)
				{
					return Optional.ofNullable(masterStore.get(domainId))
					               .map(model -> IdentifiedModel.of(domainId, model));
				}

				@Override
				public Collection<IdentifiedModel<String, MasterModel>> findByIds(final Set<String> domainIds)
				{
					return domainIds.stream().flatMap(domainId -> findById(domainId).stream()).toList();
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
			}
		}

		private final class TestSatelliteAggregateDefinition
				implements AggregateDefinition<Long, SatelliteModel, SatelliteCreate, SatellitePatch, String>
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
			public DeletionPolicy<SatelliteModel> deletionPolicy()
			{
				return _ ->
				{
				};
			}

			@Override
			public DuplicateDefinition<SatelliteModel> duplicateDefinition()
			{
				return (left, right) -> left.value().equals(right.value());
			}

			@Override
			public PostCommitMutation<Long, SatelliteModel> postCommitMutation()
			{
				return PostCommitMutation.noop();
			}

			@Override
			public Collection<AggregateRelationshipDefinitionContract<Long, SatelliteModel, SatelliteCreate, SatellitePatch>>
			relationshipDefinitions()
			{
				return List.of();
			}

			@Override
			public DomainSecurityPolicy<SatelliteModel> securityPolicy()
			{
				return DomainSecurityPolicy.allowing();
			}

			@Override
			public PatchPolicy<SatelliteModel> patchPolicy()
			{
				return (_, _) ->
				{
				};
			}

			@Override
			public InsertionPolicy<SatelliteModel> insertionPolicy()
			{
				return _ ->
				{
				};
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
					var id = (long) generatedIds.incrementAndGet();
					operationLog.add("satellite:create:" + domainModel.value());
					satelliteStore.put(id, domainModel);
					return IdentifiedModel.of(id, domainModel);
				}

				@Override
				public void put(final Long domainId, final SatelliteModel domainModel)
				{
					operationLog.add("satellite:put:" + domainId);
					satelliteStore.put(domainId, domainModel);
				}

				@Override
				public IdentifiedModel<Long, SatelliteModel> update(final Long domainId,
				                                                    final SatelliteModel domainModel)
				{
					operationLog.add("satellite:update:" + domainId);
					satelliteStore.put(domainId, domainModel);
					return IdentifiedModel.of(domainId, domainModel);
				}

				@Override
				public void delete(final Long domainId)
				{
					operationLog.add("satellite:delete:" + domainId);
					satelliteStore.remove(domainId);
				}
			}

			private final class TestSatelliteFetchPort implements AggregateFetchPort<Long, SatelliteModel>
			{
				@Override
				public Optional<IdentifiedModel<Long, SatelliteModel>> findById(final Long domainId)
				{
					return Optional.ofNullable(satelliteStore.get(domainId))
					               .map(model -> IdentifiedModel.of(domainId, model));
				}

				@Override
				public Collection<IdentifiedModel<Long, SatelliteModel>> findByIds(final Set<Long> domainIds)
				{
					return domainIds.stream().flatMap(domainId -> findById(domainId).stream()).toList();
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
			public RelationshipKind relationshipKind()
			{
				return RelationshipKind.OWNED;
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
			public AggregateDefinition<Long, SatelliteModel, SatelliteCreate, SatellitePatch, ?> satelliteDefinition()
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
			public SatelliteCreateInputResolver<MasterCreate, Collection<SatelliteCreateIntent<Long, SatelliteCreate>>> createInputResolver()
			{
				return MasterCreate::satelliteCreateIntents;
			}

			@Override
			public SatellitePatchInputResolver<MasterPatch, Collection<SatelliteMutationIntent<Long, SatelliteCreate, SatellitePatch>>> patchInputResolver()
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

	private static final class TestEngineFacade
	{
		private final AggregateLifecycle engine;
		private final AggregateDefinitionGuard definitionGuard;
		private final AggregateMutationValidationSupport validationSupport;
		private final AggregateSaveCoordinator saveCoordinator;
		private final AggregateFetchCoordinator fetchCoordinator;
		private final AggregateDeleteCoordinator deleteCoordinator;
		private final AggregateUpdateCoordinator updateCoordinator;

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		IdentifiedModel<MasterDomainId, MasterDomainModel> save(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainModelCreate model)
		{
			return saveAll(definition, List.of(model)).stream().findFirst().orElseThrow();
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> saveAll(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final Collection<MasterDomainModelCreate> models)
		{
			return engine.execute(new AggregateWorkflow<>()
			{
				@Override
				public Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> inTransaction()
				{
					var relationships = satelliteRelationships(definition);
					if (relationships.isEmpty())
					{
						var domainModels = models.stream().map(definition.createBuilder()::toModel).toList();
						validationSupport.validateSaveModels(definition, domainModels);
						return domainModels.stream().map(definition.mutationPort()::create).toList();
					}
					validationSupport.validateSaveModels(definition,
							models.stream().map(definition.createBuilder()::toModel).toList());
					return saveCoordinator.saveAll(definition, relationships, models);
				}

				@Override
				public void afterTransaction(
						final Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> result)
				{
					result.forEach(saved -> definition.postCommitMutation().accept(createContext(saved)));
				}
			});
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		List<AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, ?, ?, ?, ?>> satelliteRelationships(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition)
		{
			return definitionGuard.satelliteRelationships(definition);
		}

		private <MasterDomainId, MasterDomainModel> PostCommitMutationContext<MasterDomainId, MasterDomainModel> createContext(
				final IdentifiedModel<MasterDomainId, MasterDomainModel> saved)
		{
			return new PostCommitMutationContext<>(
					PostCommitMutationKind.CREATE,
					saved.id(),
					Optional.of(saved.model()),
					Optional.empty());
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse> void putAtId(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainId id,
				final MasterDomainModelCreate model)
		{
			engine.execute(new AggregateWorkflow<PostCommitMutationContext<MasterDomainId, MasterDomainModel>>()
			{
				@Override
				public PostCommitMutationContext<MasterDomainId, MasterDomainModel> inTransaction()
				{
					var relationships = satelliteRelationships(definition);
					var previousModel = definition.fetchPort().findById(id).map(IdentifiedModel::model);
					if (relationships.isEmpty())
					{
						var replacement = definition.createBuilder().toModel(model);
						validationSupport.validateAccess(definition, replacement);
						previousModel.ifPresentOrElse(
								current -> validationSupport.validateAccessAndValidatePatch(definition, current,
										replacement),
								() -> definition.insertionPolicy().validateInsertion(replacement));
						definition.mutationPort().put(id, replacement);
					}
					else
					{
						updateCoordinator.putAtId(definition, relationships, id, model);
					}
					var currentModel = definition.fetchPort().findById(id)
					                             .map(IdentifiedModel::model)
					                             .orElseThrow();
					return putContext(id, previousModel, currentModel);
				}

				@Override
				public void afterTransaction(final PostCommitMutationContext<MasterDomainId, MasterDomainModel> result)
				{
					definition.postCommitMutation().accept(result);
				}
			});
		}

		private <MasterDomainId, MasterDomainModel> PostCommitMutationContext<MasterDomainId, MasterDomainModel> putContext(
				final MasterDomainId id,
				final Optional<MasterDomainModel> previousModel,
				final MasterDomainModel currentModel)
		{
			return new PostCommitMutationContext<>(
					PostCommitMutationKind.PUT,
					id,
					Optional.of(currentModel),
					previousModel);
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> updateAllById(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final Collection<IdentifiedModel<MasterDomainId, MasterDomainModelUpdatePatch>> models,
				final BulkOperationMode mode)
		{
			return switch (mode)
			{
				case ALL_OR_NOTHING -> engine.execute(
						new AggregateWorkflow<Collection<UpdateDispatch<MasterDomainId, MasterDomainModel>>>()
						{
							@Override
							public Collection<UpdateDispatch<MasterDomainId, MasterDomainModel>> inTransaction()
							{
								var relationships = satelliteRelationships(definition);
								var updates = new ArrayList<UpdateDispatch<MasterDomainId, MasterDomainModel>>();
								for (var model : models)
								{
									var previousModel = definition.fetchPort().findById(model.id())
									                              .map(IdentifiedModel::model)
									                              .orElseThrow(() -> ResourceNotFoundException.withId(
																		  model.id()));
									var updated = relationships.isEmpty()
											? updateByIdWithoutRelationships(definition, model.id(), model.model())
											: updateCoordinator.updateById(definition, relationships, model.id(),
											model.model());
									updates.add(new UpdateDispatch<>(updated,
											patchContext(updated.id(), Optional.of(previousModel), updated.model())));
								}
								return updates;
							}

							@Override
							public void afterTransaction(
									final Collection<UpdateDispatch<MasterDomainId, MasterDomainModel>> result)
							{
								result.forEach(update -> definition.postCommitMutation().accept(update.context()));
							}
						}).stream().map(UpdateDispatch::updated).toList();
				case BEST_EFFORT -> models.stream()
				                          .map(model -> tryUpdateById(definition, model.id(), model.model()))
				                          .flatMap(Optional::stream)
				                          .toList();
			};
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		IdentifiedModel<MasterDomainId, MasterDomainModel> updateByIdWithoutRelationships(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainId id,
				final MasterDomainModelUpdatePatch updatePatch)
		{
			var current = definition.fetchPort().findById(id).orElseThrow(() -> ResourceNotFoundException.withId(id));
			validationSupport.validateAccess(definition, current.model());
			var updatedModel = definition.patcher().patchModel(current.model(), updatePatch);
			validationSupport.validateAccessAndValidatePatch(definition, current.model(), updatedModel);
			return definition.mutationPort().update(id, updatedModel);
		}

		private <MasterDomainId, MasterDomainModel> PostCommitMutationContext<MasterDomainId, MasterDomainModel> patchContext(
				final MasterDomainId id,
				final Optional<MasterDomainModel> previousModel,
				final MasterDomainModel currentModel)
		{
			return new PostCommitMutationContext<>(
					PostCommitMutationKind.PATCH,
					id,
					Optional.of(currentModel),
					previousModel);
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		Optional<IdentifiedModel<MasterDomainId, MasterDomainModel>> tryUpdateById(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainId id,
				final MasterDomainModelUpdatePatch updatePatch)
		{
			try
			{
				return Optional.of(updateById(definition, id, updatePatch));
			}
			catch (de.gupta.clean.crud.template.domain.model.exceptions.DomainException e)
			{
				return Optional.empty();
			}
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		IdentifiedModel<MasterDomainId, MasterDomainModel> updateById(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainId id,
				final MasterDomainModelUpdatePatch updatePatch)
		{
			return engine.execute(new AggregateWorkflow<UpdateDispatch<MasterDomainId, MasterDomainModel>>()
			{
				@Override
				public UpdateDispatch<MasterDomainId, MasterDomainModel> inTransaction()
				{
					var relationships = satelliteRelationships(definition);
					var previousModel = definition.fetchPort().findById(id)
					                              .map(IdentifiedModel::model)
					                              .orElseThrow(() -> ResourceNotFoundException.withId(id));
					var updated = relationships.isEmpty()
							? updateByIdWithoutRelationships(definition, id, updatePatch)
							: updateCoordinator.updateById(definition, relationships, id, updatePatch);
					return new UpdateDispatch<>(updated, patchContext(updated.id(), Optional.of(previousModel),
							updated.model()));
				}

				@Override
				public void afterTransaction(final UpdateDispatch<MasterDomainId, MasterDomainModel> result)
				{
					definition.postCommitMutation().accept(result.context());
				}
			}).updated();
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAll(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition)
		{
			return engine.execute(new AggregateWorkflow<>()
			{
				@Override
				public Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> inTransaction()
				{
					var relationships = satelliteRelationships(definition);
					if (relationships.isEmpty())
					{
						return definition.fetchPort().findAll().stream()
						                 .filter(model -> definition.securityPolicy().isAccessAllowed(model.model()))
						                 .toList();
					}
					return fetchCoordinator.findAll(definition, relationships);
				}

				@Override
				public boolean readOnly()
				{
					return true;
				}
			});
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		Slice<IdentifiedModel<MasterDomainId, MasterDomainModel>> findAll(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final Pageable pageable)
		{
			return engine.execute(new AggregateWorkflow<>()
			{
				@Override
				public Slice<IdentifiedModel<MasterDomainId, MasterDomainModel>> inTransaction()
				{
					var relationships = satelliteRelationships(definition);
					if (relationships.isEmpty())
					{
						return de.gupta.clean.crud.template.useCases.crud.common.utility.PageUtility.filterSlice(
								definition.fetchPort().findAll(pageable),
								model -> definition.securityPolicy().isAccessAllowed(model.model()));
					}
					return fetchCoordinator.findAll(definition, relationships, pageable);
				}

				@Override
				public boolean readOnly()
				{
					return true;
				}
			});
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		IdentifiedModel<MasterDomainId, MasterDomainModel> findById(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainId id)
		{
			return engine.execute(new AggregateWorkflow<>()
			{
				@Override
				public IdentifiedModel<MasterDomainId, MasterDomainModel> inTransaction()
				{
					var relationships = satelliteRelationships(definition);
					if (relationships.isEmpty())
					{
						return definition.fetchPort().findById(id)
						                 .filter(model -> definition.securityPolicy().isAccessAllowed(model.model()))
						                 .orElseThrow(() -> ResourceNotFoundException.withId(id));
					}
					return fetchCoordinator.findById(definition, relationships, id);
				}

				@Override
				public boolean readOnly()
				{
					return true;
				}
			});
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse>
		Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> findByIds(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final Set<MasterDomainId> ids)
		{
			return engine.execute(new AggregateWorkflow<>()
			{
				@Override
				public Collection<IdentifiedModel<MasterDomainId, MasterDomainModel>> inTransaction()
				{
					var relationships = satelliteRelationships(definition);
					if (relationships.isEmpty())
					{
						return definition.fetchPort().findByIds(ids).stream()
						                 .filter(model -> definition.securityPolicy().isAccessAllowed(model.model()))
						                 .toList();
					}
					return fetchCoordinator.findByIds(definition, relationships, ids);
				}

				@Override
				public boolean readOnly()
				{
					return true;
				}
			});
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse> void deleteAllById(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final Collection<MasterDomainId> ids,
				final BulkOperationMode mode)
		{
			switch (mode)
			{
				case ALL_OR_NOTHING -> engine.execute(
						new AggregateWorkflow<Collection<PostCommitMutationContext<MasterDomainId, MasterDomainModel>>>()
						{
							@Override
							public Collection<PostCommitMutationContext<MasterDomainId, MasterDomainModel>> inTransaction()
							{
								var relationships = satelliteRelationships(definition);
								var deleted =
										new ArrayList<PostCommitMutationContext<MasterDomainId, MasterDomainModel>>();
								for (var id : ids)
								{
									var previousModel = definition.fetchPort().findById(id)
									                              .map(IdentifiedModel::model)
									                              .orElseThrow(() -> ResourceNotFoundException.withId(
																		  id));
									if (relationships.isEmpty())
									{
										validationSupport.validateDeletion(definition, previousModel);
										definition.mutationPort().delete(id);
									}
									else
									{
										deleteCoordinator.deleteById(definition, relationships, id);
									}
									deleted.add(deleteContext(id, previousModel));
								}
								return deleted;
							}

							@Override
							public void afterTransaction(
									final Collection<PostCommitMutationContext<MasterDomainId, MasterDomainModel>> result)
							{
								result.forEach(definition.postCommitMutation());
							}
						});
				case BEST_EFFORT -> ids.forEach(id -> tryDeleteById(definition, id));
			}
		}

		private <MasterDomainId, MasterDomainModel> PostCommitMutationContext<MasterDomainId, MasterDomainModel> deleteContext(
				final MasterDomainId id,
				final MasterDomainModel previousModel)
		{
			return new PostCommitMutationContext<>(
					PostCommitMutationKind.DELETE,
					id,
					Optional.empty(),
					Optional.of(previousModel));
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse> void tryDeleteById(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainId id)
		{
			try
			{
				deleteById(definition, id);
			}
			catch (de.gupta.clean.crud.template.domain.model.exceptions.DomainException ignored)
			{
			}
		}

		private <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
				MasterDomainModelResponse> void deleteById(
				final AggregateDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
						MasterDomainModelUpdatePatch, MasterDomainModelResponse> definition,
				final MasterDomainId id)
		{
			engine.execute(new AggregateWorkflow<PostCommitMutationContext<MasterDomainId, MasterDomainModel>>()
			{
				@Override
				public PostCommitMutationContext<MasterDomainId, MasterDomainModel> inTransaction()
				{
					var relationships = satelliteRelationships(definition);
					var previousModel = definition.fetchPort().findById(id)
					                              .map(IdentifiedModel::model)
					                              .orElseThrow(() -> ResourceNotFoundException.withId(id));
					if (relationships.isEmpty())
					{
						validationSupport.validateDeletion(definition, previousModel);
						definition.mutationPort().delete(id);
					}
					else
					{
						deleteCoordinator.deleteById(definition, relationships, id);
					}
					return deleteContext(id, previousModel);
				}

				@Override
				public void afterTransaction(final PostCommitMutationContext<MasterDomainId, MasterDomainModel> result)
				{
					definition.postCommitMutation().accept(result);
				}
			});
		}

		private TestEngineFacade(final AggregateLifecycle engine)
		{
			this.engine = engine;
			this.definitionGuard = new AggregateDefinitionGuard();
			this.validationSupport = new AggregateMutationValidationSupport();
			var relationshipPlanner = new SatelliteRelationshipPlanner();
			var referenceResolver = new SatelliteReferenceResolver();
			var createIntentResolver = SatelliteCreateIntentResolver.with(relationshipPlanner, referenceResolver);
			this.saveCoordinator = AggregateSaveCoordinator.with(relationshipPlanner, createIntentResolver);
			this.fetchCoordinator = AggregateFetchCoordinator.create();
			this.deleteCoordinator = AggregateDeleteCoordinator.with(relationshipPlanner, referenceResolver);
			this.updateCoordinator =
					AggregateUpdateCoordinator.with(relationshipPlanner, referenceResolver, createIntentResolver,
							validationSupport);
		}

		private record UpdateDispatch<DomainId, DomainModel>(
				IdentifiedModel<DomainId, DomainModel> updated,
				PostCommitMutationContext<DomainId, DomainModel> context)
		{
		}
	}

	private static final class TestTransactionRunner implements PersistenceTransactionRunner
	{
		private int transactionCount;

		@Override
		public <T> T inTransaction(final Supplier<T> action)
		{
			transactionCount++;
			return action.get();
		}

		int transactionCount()
		{
			return transactionCount;
		}
	}

	private static class RecordingDispatcher implements PostCommitMutationDispatcher
	{
		private final List<PostCommitMutationContext<String, MasterModel>> contexts = new ArrayList<>();

		@Override
		@SuppressWarnings("unchecked")
		public <DomainId, DomainModel> void dispatch(
				final PostCommitMutation<DomainId, DomainModel> postCommitMutation,
				final PostCommitMutationContext<DomainId, DomainModel> context)
		{
			contexts.add((PostCommitMutationContext<String, MasterModel>) context);
			postCommitMutation.accept(context);
		}

	}

	private static final class SwallowingRecordingDispatcher extends RecordingDispatcher
	{
		@Override
		public <DomainId, DomainModel> void dispatch(
				final PostCommitMutation<DomainId, DomainModel> postCommitMutation,
				final PostCommitMutationContext<DomainId, DomainModel> context)
		{
			try
			{
				super.dispatch(postCommitMutation, context);
			}
			catch (RuntimeException ignored)
			{
			}
		}

		@Override
		public void dispatch(final Runnable action)
		{
			try
			{
				action.run();
			}
			catch (RuntimeException ignored)
			{
			}
		}
	}
}