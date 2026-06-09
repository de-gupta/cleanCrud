package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutation;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinitionContract;
import de.gupta.clean.crud.template.useCases.crud.aggregate.service.AggregateCrudServices;
import de.gupta.clean.crud.template.useCases.crud.delete.application.service.AbstractDeleteService;
import de.gupta.clean.crud.template.useCases.crud.fetch.application.service.AbstractFetchService;
import de.gupta.clean.crud.template.useCases.crud.save.application.service.AbstractSaveService;
import de.gupta.clean.crud.template.useCases.crud.update.application.service.AbstractUpdateService;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStarter;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessDefinition;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessTrigger;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.CorrelationId;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.BackoffPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.RetryPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class AbstractCrudServicesEngineBackedTest
{
	@Test
	void saveAndUpdateServicesMapDomainResultsToResponseModels()
	{
		TestAggregateDefinition definition = new TestAggregateDefinition();
		DefaultAggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());

		TestSaveService saveService = new TestSaveService(definition, engine);
		TestUpdateService updateService = new TestUpdateService(definition, engine);

		assertEquals("response:saved", saveService.save("saved").model());
		definition.store.put("id", "before");
		assertEquals("response:patched", updateService.updateById("id", "patched").model());
	}

	@Test
	void fetchAndDeleteServicesPreserveDomainBehavior()
	{
		TestAggregateDefinition definition = new TestAggregateDefinition();
		definition.store.put("id", "value");
		DefaultAggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());

		TestFetchService fetchService = new TestFetchService(definition, engine);
		TestDeleteService deleteService = new TestDeleteService(definition, engine);

		assertEquals("value", fetchService.findById("id").model());
		deleteService.deleteById("id");
		assertEquals(List.of("id"), definition.deletedIds);
	}

	@Test
	void servicesDispatchPostCommitMutationsAsynchronously()
			throws InterruptedException
	{
		TestAggregateDefinition definition = new TestAggregateDefinition();
		CountDownLatch latch = new CountDownLatch(3);
		java.util.List<PostCommitMutationContext<String, String>> contexts =
				java.util.Collections.synchronizedList(new java.util.ArrayList<>());
		definition.postCommitMutation = context ->
		{
			contexts.add(context);
			latch.countDown();
		};
		DefaultAggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());

		TestSaveService saveService = new TestSaveService(definition, engine);
		TestUpdateService updateService = new TestUpdateService(definition, engine);
		TestDeleteService deleteService = new TestDeleteService(definition, engine);

		saveService.save("saved");
		definition.store.put("id", "before");
		updateService.updateById("id", "patched");
		deleteService.deleteById("id");

		assertTrue(latch.await(2, TimeUnit.SECONDS));
		assertEquals(
				List.of(PostCommitMutationKind.CREATE, PostCommitMutationKind.PATCH, PostCommitMutationKind.DELETE),
				contexts.stream().map(PostCommitMutationContext::kind).toList());
	}

	@Test
	void updateServiceThrowsResourceNotFoundWhenPatchingMissingModel()
	{
		TestAggregateDefinition definition = new TestAggregateDefinition();
		DefaultAggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());

		TestUpdateService updateService = new TestUpdateService(definition, engine);

		assertThrows(ResourceNotFoundException.class, () -> updateService.updateById("missing", "patched"));
	}

	@Test
	void deleteServiceThrowsResourceNotFoundWhenDeletingMissingModel()
	{
		TestAggregateDefinition definition = new TestAggregateDefinition();
		DefaultAggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());

		TestDeleteService deleteService = new TestDeleteService(definition, engine);

		assertThrows(ResourceNotFoundException.class, () -> deleteService.deleteById("missing"));
	}

	@Test
	void aggregateCrudServicesSaveServiceCanStartDurableProcesses()
	{
		TestAggregateDefinition definition = new TestAggregateDefinition();
		var startedRequests = new java.util.ArrayList<DurableProcessStartRequest<?, ?>>();
		var engine = DefaultAggregateLifecycleEngine.withTransactionRunnerAndDurableProcessStarter(
				new InlineTransactionRunner(),
				new RecordingDurableProcessStarter(startedRequests));
		var processDefinition = DurableProcessDefinition.of("test-process", SavedTrigger.class, SavedPayload.class);
		var retryPolicy = new RetryPolicy(2, BackoffPolicy.fixed(java.time.Duration.ofMillis(5)));

		var saveService = AggregateCrudServices.saveService(
				definition,
				engine,
				savedModels -> savedModels.stream()
				                          .map(saved -> new DurableProcessStartRequest<>(
												  processDefinition,
												  new SavedTrigger(saved.id()),
												  new SavedPayload(saved.model()),
												  new CorrelationId("saved:" + saved.id()),
												  retryPolicy))
				                          .<DurableProcessStartRequest<?, ?>>map(request -> request)
				                          .toList());

		saveService.save("saved");

		assertEquals(1, startedRequests.size());
		assertEquals("test-process", startedRequests.getFirst().definition().processType());
	}

	@Test
	void aggregateCrudServicesUpdateServiceCanStartDurableProcesses()
	{
		TestAggregateDefinition definition = new TestAggregateDefinition();
		definition.store.put("id", "before");
		var startedRequests = new java.util.ArrayList<DurableProcessStartRequest<?, ?>>();
		var engine = DefaultAggregateLifecycleEngine.withTransactionRunnerAndDurableProcessStarter(
				new InlineTransactionRunner(),
				new RecordingDurableProcessStarter(startedRequests));
		var processDefinition = DurableProcessDefinition.of("test-update-process", SavedTrigger.class,
				SavedPayload.class);
		var retryPolicy = new RetryPolicy(2, BackoffPolicy.fixed(java.time.Duration.ofMillis(5)));

		var updateService = AggregateCrudServices.updateService(
				definition,
				engine,
				context -> List.of(new DurableProcessStartRequest<>(
						processDefinition,
						new SavedTrigger(context.domainId()),
						new SavedPayload(context.currentModel().orElseThrow()),
						new CorrelationId("updated:" + context.domainId()),
						retryPolicy)));

		updateService.updateById("id", "patched");

		assertEquals(1, startedRequests.size());
		assertEquals("test-update-process", startedRequests.getFirst().definition().processType());
	}

	@Test
	void aggregateCrudServicesDeleteServiceCanStartDurableProcesses()
	{
		TestAggregateDefinition definition = new TestAggregateDefinition();
		definition.store.put("id", "before");
		var startedRequests = new java.util.ArrayList<DurableProcessStartRequest<?, ?>>();
		var engine = DefaultAggregateLifecycleEngine.withTransactionRunnerAndDurableProcessStarter(
				new InlineTransactionRunner(),
				new RecordingDurableProcessStarter(startedRequests));
		var processDefinition = DurableProcessDefinition.of("test-delete-process", SavedTrigger.class,
				SavedPayload.class);
		var retryPolicy = new RetryPolicy(2, BackoffPolicy.fixed(java.time.Duration.ofMillis(5)));

		var deleteService = AggregateCrudServices.deleteService(
				definition,
				engine,
				context -> List.of(new DurableProcessStartRequest<>(
						processDefinition,
						new SavedTrigger(context.domainId()),
						new SavedPayload(context.previousModel().orElseThrow()),
						new CorrelationId("deleted:" + context.domainId()),
						retryPolicy)));

		deleteService.deleteById("id");

		assertEquals(1, startedRequests.size());
		assertEquals("test-delete-process", startedRequests.getFirst().definition().processType());
	}

	private static final class TestSaveService
			extends AbstractSaveService<String, String, String, String, String>
	{
		private TestSaveService(
				final AggregateCrudDefinition<String, String, String, String, String> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
					AggregateServiceSupportFactory.validationSupport(),
					AggregateServiceSupportFactory.saveCoordinator());
		}
	}

	private static final class TestUpdateService
			extends AbstractUpdateService<String, String, String, String, String>
	{
		private TestUpdateService(
				final AggregateCrudDefinition<String, String, String, String, String> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
					AggregateServiceSupportFactory.validationSupport(),
					AggregateServiceSupportFactory.updateCoordinator());
		}
	}

	private static final class TestFetchService
			extends AbstractFetchService<String, String, String, String, String>
	{
		private TestFetchService(
				final AggregateCrudDefinition<String, String, String, String, String> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
					AggregateServiceSupportFactory.fetchCoordinator());
		}
	}

	private static final class TestDeleteService
			extends AbstractDeleteService<String, String, String, String, String>
	{
		private TestDeleteService(
				final AggregateCrudDefinition<String, String, String, String, String> definition,
				final AggregateLifecycleEngine engine)
		{
			super(definition, engine, AggregateServiceSupportFactory.definitionGuard(),
					AggregateServiceSupportFactory.validationSupport(),
					AggregateServiceSupportFactory.deleteCoordinator());
		}
	}

	private static final class TestAggregateDefinition
			implements AggregateCrudDefinition<String, String, String, String, String>
	{
		private final java.util.Map<String, String> store = new java.util.HashMap<>();
		private final java.util.List<String> deletedIds = new java.util.ArrayList<>();
		private PostCommitMutation<String, String> postCommitMutation = PostCommitMutation.noop();

		@Override
		public AggregateMutationPort<String, String, String, String> mutationPort()
		{
			return new AggregateMutationPort<>()
			{
				@Override
				public IdentifiedModel<String, String> create(final String domainModel)
				{
					store.put("saved", domainModel);
					return IdentifiedModel.of("saved", domainModel);
				}

				@Override
				public void put(final String domainId, final String domainModel)
				{
					store.put(domainId, domainModel);
				}

				@Override
				public IdentifiedModel<String, String> update(final String domainId, final String domainModel)
				{
					store.put(domainId, domainModel);
					return IdentifiedModel.of(domainId, domainModel);
				}

				@Override
				public void delete(final String domainId)
				{
					deletedIds.add(domainId);
				}
			};
		}

		@Override
		public AggregateFetchPort<String, String> fetchPort()
		{
			return new AggregateFetchPort<>()
			{
				@Override
				public Optional<IdentifiedModel<String, String>> findById(final String domainId)
				{
					return Optional.ofNullable(store.get(domainId)).map(model -> IdentifiedModel.of(domainId, model));
				}

				@Override
				public Collection<IdentifiedModel<String, String>> findByIds(final Set<String> domainIds)
				{
					return domainIds.stream().flatMap(id -> findById(id).stream()).toList();
				}

				@Override
				public Collection<IdentifiedModel<String, String>> findAll()
				{
					return store.entrySet().stream().map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue()))
					            .toList();
				}

				@Override
				public Slice<IdentifiedModel<String, String>> findAll(final Pageable pageable)
				{
					return new SliceImpl<>(findAll().stream().toList());
				}
			};
		}

		@Override
		public DomainModelBuilder<String, String> createBuilder()
		{
			return value -> value;
		}

		@Override
		public DomainModelPatcher<String, String> patcher()
		{
			return (_, patch) -> patch;
		}

		@Override
		public DomainResponseBuilder<String, String> responseBuilder()
		{
			return value -> "response:" + value;
		}

		@Override
		public InsertionPolicy<String> insertionPolicy()
		{
			return _ ->
			{
			};
		}

		@Override
		public PatchPolicy<String> patchPolicy()
		{
			return (_, _) ->
			{
			};
		}

		@Override
		public DeletionPolicy<String> deletionPolicy()
		{
			return _ ->
			{
			};
		}

		@Override
		public DomainSecurityPolicy<String> securityPolicy()
		{
			return DomainSecurityPolicy.allowing();
		}

		@Override
		public DuplicateDefinition<String> duplicateDefinition()
		{
			return String::equals;
		}

		@Override
		public PostCommitMutation<String, String> postCommitMutation()
		{
			return postCommitMutation;
		}

		@Override
		public Collection<AggregateRelationshipDefinitionContract<String, String, String, String>>
		relationshipDefinitions()
		{
			return List.of();
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

	private record SavedTrigger(String id) implements DurableProcessTrigger
	{
	}

	private record SavedPayload(String value) implements DurableProcessPayload
	{
	}

	private record RecordingDurableProcessStarter(java.util.List<DurableProcessStartRequest<?, ?>> startedRequests)
			implements DurableProcessStarter
	{
		@Override
		public DurableProcessTaskId start(final DurableProcessStartRequest<?, ?> startRequest)
		{
			startedRequests.add(startRequest);
			return DurableProcessTaskId.random();
		}
	}
}