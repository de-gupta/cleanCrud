package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceCannotBeDeletedException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceCannotBePatchedException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.equality.KeyBasedDuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinitionContract;
import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAggregateLifecycleEngineTest
{
	@Test
	void saveValidatesAccessInsertionAndDuplicateRules()
	{
		TestTransactionRunner runner = new TestTransactionRunner();
		DefaultAggregateLifecycleEngine engine = new DefaultAggregateLifecycleEngine(runner);
		TestAggregateDefinition definition = TestAggregateDefinition.standard();

		var saved = engine.save(definition, "alpha");

		assertEquals(1, runner.transactionCount());
		assertEquals("generated-1", saved.id());
		assertEquals("alpha", saved.model().value());
		assertEquals(List.of("alpha"), definition.insertionValidatedValues);

		assertThrows(RuntimeException.class, () ->
				engine.save(TestAggregateDefinition.withSecurity(_ -> false), "denied"));
		assertThrows(RuntimeException.class, () ->
				engine.saveAll(definition, List.of("same", "same")));
	}

	@Test
	void putAtIdChoosesInsertionOrPatchBehaviorBasedOnCurrentState()
	{
		DefaultAggregateLifecycleEngine engine = new DefaultAggregateLifecycleEngine(new TestTransactionRunner());
		TestAggregateDefinition absentDefinition = TestAggregateDefinition.standard();

		engine.putAtId(absentDefinition, "42", "new");

		assertEquals(List.of("new"), absentDefinition.insertionValidatedValues);
		assertEquals(List.of("42"), absentDefinition.putIds);

		TestAggregateDefinition currentDefinition = TestAggregateDefinition.standard();
		currentDefinition.store.put("42", TestModel.of("current"));

		engine.putAtId(currentDefinition, "42", "replacement");

		assertEquals(List.of("current->replacement"), currentDefinition.patchValidatedPairs);
	}

	@Test
	void updateByIdFetchesPatchesValidatesAndPersists()
	{
		DefaultAggregateLifecycleEngine engine = new DefaultAggregateLifecycleEngine(new TestTransactionRunner());
		TestAggregateDefinition definition = TestAggregateDefinition.standard();
		definition.store.put("1", TestModel.of("before"));

		var updated = engine.updateById(definition, "1", "after");

		assertEquals("after", updated.model().value());
		assertEquals(List.of("before->after"), definition.patchValidatedPairs);
		assertEquals(List.of("1"), definition.updatedIds);
	}

	@Test
	void findByIdEnforcesVisibilityAndDeleteByIdEnforcesDeletionPolicy()
	{
		DefaultAggregateLifecycleEngine engine = new DefaultAggregateLifecycleEngine(new TestTransactionRunner());
		TestAggregateDefinition definition = TestAggregateDefinition.standard();
		definition.store.put("1", TestModel.of("visible"));

		assertEquals("visible", engine.findById(definition, "1").model().value());

		TestAggregateDefinition hiddenDefinition = TestAggregateDefinition.withSecurity(model -> false);
		hiddenDefinition.store.put("1", TestModel.of("hidden"));
		assertThrows(RuntimeException.class, () -> engine.findById(hiddenDefinition, "1"));

		TestAggregateDefinition deleteBlocked = TestAggregateDefinition.standard();
		deleteBlocked.store.put("1", TestModel.of("protected"));
		deleteBlocked.deletionPolicy = model ->
		{
			throw ResourceCannotBeDeletedException.withMessage("blocked");
		};
		assertThrows(ResourceCannotBeDeletedException.class, () -> engine.deleteById(deleteBlocked, "1"));
	}

	@Test
	void nonEmptyRelationshipDefinitionsAreRejected()
	{
		DefaultAggregateLifecycleEngine engine = new DefaultAggregateLifecycleEngine(new TestTransactionRunner());
		TestAggregateDefinition definition = TestAggregateDefinition.standard();
		definition.relationshipDefinitions = List.of(new TestRelationshipContract());

		var exception = assertThrows(UnsupportedOperationException.class, () -> engine.save(definition, "value"));
		assertTrue(exception.getMessage().contains("phase 2"));
	}

	@Test
	void bulkModesPreserveAllOrNothingAndBestEffortSemanticsAndTransactions()
	{
		TestTransactionRunner updateRunner = new TestTransactionRunner();
		DefaultAggregateLifecycleEngine updateEngine = new DefaultAggregateLifecycleEngine(updateRunner);
		TestAggregateDefinition updateDefinition = TestAggregateDefinition.standard();
		updateDefinition.store.put("ok", TestModel.of("start"));
		updateDefinition.store.put("bad", TestModel.of("blocked"));
		updateDefinition.patchPolicy = (original, replacement) ->
		{
			if ("blocked".equals(original.value()))
			{
				throw ResourceCannotBePatchedException.withMessage("blocked");
			}
		};

		assertThrows(ResourceCannotBePatchedException.class, () -> updateEngine.updateAllById(
				updateDefinition,
				List.of(
						IdentifiedModel.of("ok", "ok-update"),
						IdentifiedModel.of("bad", "bad-update")),
				BulkOperationMode.ALL_OR_NOTHING));
		assertEquals(1, updateRunner.transactionCount());

		updateRunner.reset();
		var bestEffortUpdates = updateEngine.updateAllById(
				updateDefinition,
				List.of(
						IdentifiedModel.of("ok", "ok-update"),
						IdentifiedModel.of("bad", "bad-update")),
				BulkOperationMode.BEST_EFFORT);
		assertEquals(1, bestEffortUpdates.size());
		assertEquals(2, updateRunner.transactionCount());

		TestTransactionRunner deleteRunner = new TestTransactionRunner();
		DefaultAggregateLifecycleEngine deleteEngine = new DefaultAggregateLifecycleEngine(deleteRunner);
		TestAggregateDefinition deleteDefinition = TestAggregateDefinition.standard();
		deleteDefinition.store.put("good", TestModel.of("good"));
		deleteDefinition.store.put("blocked", TestModel.of("blocked"));
		deleteDefinition.deletionPolicy = model ->
		{
			if ("blocked".equals(model.value()))
			{
				throw ResourceCannotBeDeletedException.withMessage("blocked");
			}
		};

		assertThrows(ResourceCannotBeDeletedException.class, () -> deleteEngine.deleteAllById(
				deleteDefinition,
				List.of("good", "blocked"),
				BulkOperationMode.ALL_OR_NOTHING));
		assertEquals(1, deleteRunner.transactionCount());

		deleteRunner.reset();
		deleteEngine.deleteAllById(deleteDefinition, List.of("good", "blocked"), BulkOperationMode.BEST_EFFORT);
		assertEquals(2, deleteRunner.transactionCount());
		assertTrue(deleteDefinition.deletedIds.contains("good"));
	}

	private static final class TestAggregateDefinition
			implements AggregateCrudDefinition<String, TestModel, String, String, String>
	{
		private final Map<String, TestModel> store = new HashMap<>();
		private final AtomicInteger generatedIds = new AtomicInteger();
		private final TestMutationPort mutationPort = new TestMutationPort();
		private final TestFetchPort fetchPort = new TestFetchPort();
		private final DomainModelBuilder<String, TestModel> createBuilder = TestModel::of;
		private final DomainModelPatcher<TestModel, String> patcher = (_, patch) -> TestModel.of(patch);
		private final DomainResponseBuilder<TestModel, String> responseBuilder = TestModel::value;
		private final DuplicateDefinition<TestModel> duplicateDefinition =
				(KeyBasedDuplicateDefinition<TestModel, String>) TestModel::value;
		private final List<String> insertionValidatedValues = new ArrayList<>();
		private final List<String> patchValidatedPairs = new ArrayList<>();
		private final List<String> putIds = new ArrayList<>();
		private final List<String> updatedIds = new ArrayList<>();
		private final List<String> deletedIds = new ArrayList<>();
		private final InsertionPolicy<TestModel> insertionPolicy = model -> insertionValidatedValues.add(model.value());
		private DomainSecurityPolicy<TestModel> securityPolicy = DomainSecurityPolicy.allowing();
		private PatchPolicy<TestModel> patchPolicy =
				(original, replacement) -> patchValidatedPairs.add(original.value() + "->" + replacement.value());
		private DeletionPolicy<TestModel> deletionPolicy = model ->
		{
		};
		private Collection<AggregateRelationshipDefinitionContract<String, TestModel, String, String>>
				relationshipDefinitions = List.of();

		static TestAggregateDefinition standard()
		{
			return new TestAggregateDefinition();
		}

		static TestAggregateDefinition withSecurity(final DomainSecurityPolicy<TestModel> securityPolicy)
		{
			TestAggregateDefinition definition = new TestAggregateDefinition();
			definition.securityPolicy = securityPolicy;
			return definition;
		}

		@Override
		public AggregateMutationPort<String, TestModel, String, String> mutationPort()
		{
			return mutationPort;
		}

		@Override
		public AggregateFetchPort<String, TestModel> fetchPort()
		{
			return fetchPort;
		}

		@Override
		public DomainModelBuilder<String, TestModel> createBuilder()
		{
			return createBuilder;
		}

		@Override
		public DomainModelPatcher<TestModel, String> patcher()
		{
			return patcher;
		}

		@Override
		public DomainResponseBuilder<TestModel, String> responseBuilder()
		{
			return responseBuilder;
		}

		@Override
		public InsertionPolicy<TestModel> insertionPolicy()
		{
			return insertionPolicy;
		}

		@Override
		public PatchPolicy<TestModel> patchPolicy()
		{
			return patchPolicy;
		}

		@Override
		public DeletionPolicy<TestModel> deletionPolicy()
		{
			return deletionPolicy;
		}

		@Override
		public DomainSecurityPolicy<TestModel> securityPolicy()
		{
			return securityPolicy;
		}

		@Override
		public DuplicateDefinition<TestModel> duplicateDefinition()
		{
			return duplicateDefinition;
		}

		@Override
		public Collection<AggregateRelationshipDefinitionContract<String, TestModel, String, String>>
		relationshipDefinitions()
		{
			return relationshipDefinitions;
		}

		private final class TestMutationPort implements AggregateMutationPort<String, TestModel, String, String>
		{
			@Override
			public IdentifiedModel<String, TestModel> create(final TestModel domainModel)
			{
				String id = "generated-" + generatedIds.incrementAndGet();
				store.put(id, domainModel);
				return IdentifiedModel.of(id, domainModel);
			}

			@Override
			public void put(final String domainId, final TestModel domainModel)
			{
				putIds.add(domainId);
				store.put(domainId, domainModel);
			}

			@Override
			public IdentifiedModel<String, TestModel> update(final String domainId, final TestModel domainModel)
			{
				updatedIds.add(domainId);
				store.put(domainId, domainModel);
				return IdentifiedModel.of(domainId, domainModel);
			}

			@Override
			public void delete(final String domainId)
			{
				deletedIds.add(domainId);
				store.remove(domainId);
			}
		}

		private final class TestFetchPort implements AggregateFetchPort<String, TestModel>
		{
			@Override
			public Optional<IdentifiedModel<String, TestModel>> findById(final String domainId)
			{
				return Optional.ofNullable(store.get(domainId)).map(model -> IdentifiedModel.of(domainId, model));
			}

			@Override
			public Collection<IdentifiedModel<String, TestModel>> findByIds(final Set<String> domainIds)
			{
				return domainIds.stream().flatMap(domainId -> findById(domainId).stream()).toList();
			}

			@Override
			public Collection<IdentifiedModel<String, TestModel>> findAll()
			{
				return store.entrySet().stream().map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue()))
				            .toList();
			}

			@Override
			public Slice<IdentifiedModel<String, TestModel>> findAll(final Pageable pageable)
			{
				return new SliceImpl<>(findAll().stream().toList());
			}
		}
	}

	private record TestModel(String value)
	{
		private static TestModel of(final String value)
		{
			return new TestModel(value);
		}
	}

	private static final class TestRelationshipContract
			implements AggregateRelationshipDefinitionContract<String, TestModel, String, String>
	{
		@Override
		public String name()
		{
			return "satellite";
		}

		@Override
		public de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.Cardinality cardinality()
		{
			return de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.Cardinality.ONE;
		}

		@Override
		public de.gupta.clean.crud.template.useCases.crud.aggregate.lifecycle.LifecycleSemantics lifecycleSemantics()
		{
			return de.gupta.clean.crud.template.useCases.crud.aggregate.lifecycle.LifecycleSemantics.none();
		}

		@Override
		public de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.SatelliteCreateInputResolver<String,
				? extends de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent<?, ?>>
		createInputResolver()
		{
			return _ -> new de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent.NoSatelliteCreateIntent<>();
		}

		@Override
		public de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.SatellitePatchInputResolver<String,
				? extends Collection<? extends de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent<?, ?, ?>>>
		patchInputResolver()
		{
			return _ -> List.of();
		}

		@Override
		public de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.ReconciliationStrategy reconciliationStrategy()
		{
			return de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.ReconciliationStrategy.REPLACE;
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

		void reset()
		{
			transactionCount = 0;
		}
	}
}