package de.gupta.clean.crud.template.useCases.mutation.aggregate.service;

import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
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
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateRelationshipExecutionNotSupportedException;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.DefaultAggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.*;
import de.gupta.clean.crud.template.useCases.mutation.application.service.MutationService;
import de.gupta.clean.crud.template.useCases.mutation.domain.handler.MutationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.mutation.domain.handler.RegisteredMutationHandler;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.ApplicationMutationPayload;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;
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

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class AggregateMutationServicesTest
{
	@Test
	void mutationServiceAppliesRegisteredHandlerAndPersistsUpdatedModel()
	{
		var definition = new TestAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		AggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		var service = mutationService(definition, engine);

		var updated = service.mutate(new MutationRequest<>(
				"order-1",
				new AcknowledgeOrder(),
				MutationSource.AUTHORITATIVE_EXTERNAL_EVENT));

		assertEquals("order-1", updated.id());
		assertEquals("ACKNOWLEDGED", updated.model().status());
		assertEquals("ACKNOWLEDGED", definition.store.get("order-1").status());
	}

	@Test
	void mutationServiceDispatchesPatchShapedPostCommitMutation()
			throws InterruptedException
	{
		var definition = new TestAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		var contexts = Collections.synchronizedList(new ArrayList<PostCommitMutationContext<String, OrderModel>>());
		var latch = new CountDownLatch(1);
		definition.postCommitMutation = context ->
		{
			contexts.add(context);
			latch.countDown();
		};
		AggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		var service = mutationService(definition, engine);

		service.mutate(new MutationRequest<>("order-1", new AcknowledgeOrder(), MutationSource.INTERNAL_COMMAND));

		assertTrue(latch.await(2, TimeUnit.SECONDS));
		assertEquals(1, contexts.size());
		assertEquals(PostCommitMutationKind.PATCH, contexts.getFirst().kind());
		assertEquals("SUBMITTED", contexts.getFirst().previousModel().orElseThrow().status());
		assertEquals("ACKNOWLEDGED", contexts.getFirst().currentModel().orElseThrow().status());
	}

	@Test
	void mutationServiceCanStartDurableProcessesUsingMutationContext()
	{
		var definition = new TestAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		var startedRequests = new ArrayList<DurableProcessStartRequest<?, ?>>();
		var engine = DefaultAggregateLifecycleEngine.withTransactionRunnerAndDurableProcessStarter(
				new InlineTransactionRunner(),
				new RecordingDurableProcessStarter(startedRequests));
		var processDefinition = DurableProcessDefinition.of("order-follow-up", OrderMutated.class, OrderPayload.class);
		var retryPolicy = new RetryPolicy(3, BackoffPolicy.fixed(Duration.ofSeconds(1)));
		var service = AggregateMutationServices.mutationService(
				definition,
				engine,
				registry(),
				context -> List.of(new DurableProcessStartRequest<>(
						processDefinition,
						new OrderMutated(context.domainId()),
						new OrderPayload(context.afterModel().orElseThrow().status()),
						new CorrelationId("mutation:" + context.domainId()),
						retryPolicy)));

		service.mutate(new MutationRequest<>("order-1", new AcknowledgeOrder(), MutationSource.INTERNAL_COMMAND));

		assertEquals(1, startedRequests.size());
		assertEquals("order-follow-up", startedRequests.getFirst().definition().processType());
	}

	@Test
	void mutationServiceRejectsUnknownPayloadType()
	{
		var definition = new TestAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		AggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		var service = AggregateMutationServices.mutationService(definition, engine,
				MutationHandlerRegistry.of(List.of()));

		var exception = assertThrows(
				InvalidRequestException.class,
				() -> service.mutate(new MutationRequest<>(
						"order-1",
						new RejectOrder("risk"),
						MutationSource.AUTHORITATIVE_EXTERNAL_EVENT)));

		assertTrue(exception.getMessage().contains(RejectOrder.class.getName()));
	}

	@Test
	void mutationServiceRejectsMissingAggregate()
	{
		var definition = new TestAggregateDefinition();
		AggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		var service = mutationService(definition, engine);

		assertThrows(
				ResourceNotFoundException.class,
				() -> service.mutate(new MutationRequest<>(
						"missing",
						new AcknowledgeOrder(),
						MutationSource.AUTHORITATIVE_EXTERNAL_EVENT)));
	}

	@Test
	void mutationServiceRejectsRelationshipAggregatesForNow()
	{
		var definition = new RelationshipAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		AggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		var service = mutationService(definition, engine);

		var exception = assertThrows(
				AggregateRelationshipExecutionNotSupportedException.class,
				() -> service.mutate(new MutationRequest<>(
						"order-1",
						new AcknowledgeOrder(),
						MutationSource.INTERNAL_COMMAND)));

		assertEquals("Aggregate mutation service currently supports only aggregates without relationships",
				exception.getMessage());
	}

	@Test
	void registryRejectsDuplicatePayloadTypeRegistrations()
	{
		assertThrows(
				IllegalArgumentException.class,
				() -> MutationHandlerRegistry.of(List.of(
						RegisteredMutationHandler.of(AcknowledgeOrder.class, (OrderModel currentModel,
						                                                      AcknowledgeOrder ignored) -> new OrderModel(
								"ACKNOWLEDGED")),
						RegisteredMutationHandler.of(AcknowledgeOrder.class, (OrderModel currentModel,
						                                                      AcknowledgeOrder ignored) -> currentModel))));
	}

	private MutationService<String, OrderModel> mutationService(
			final TestAggregateDefinition definition,
			final AggregateLifecycleEngine engine)
	{
		return AggregateMutationServices.mutationService(definition, engine, registry());
	}

	private MutationHandlerRegistry<OrderModel> registry()
	{
		return MutationHandlerRegistry.of(List.of(
				RegisteredMutationHandler.of(AcknowledgeOrder.class,
						(currentModel, _) -> new OrderModel("ACKNOWLEDGED"))));
	}

	private record OrderModel(String status)
	{
	}

	private record AcknowledgeOrder() implements ApplicationMutationPayload
	{
	}

	private record RejectOrder(String reason) implements ApplicationMutationPayload
	{
	}

	private record OrderMutated(String id) implements DurableProcessTrigger
	{
	}

	private record OrderPayload(String status) implements DurableProcessPayload
	{
	}

	private static class TestAggregateDefinition
			implements AggregateCrudDefinition<String, OrderModel, String, String, String>
	{
		protected final Map<String, OrderModel> store = new LinkedHashMap<>();
		private PostCommitMutation<String, OrderModel> postCommitMutation = PostCommitMutation.noop();

		@Override
		public AggregateMutationPort<String, OrderModel, String, String> mutationPort()
		{
			return new AggregateMutationPort<>()
			{
				@Override
				public IdentifiedModel<String, OrderModel> create(final OrderModel domainModel)
				{
					store.put("created", domainModel);
					return IdentifiedModel.of("created", domainModel);
				}

				@Override
				public void put(final String domainId, final OrderModel domainModel)
				{
					store.put(domainId, domainModel);
				}

				@Override
				public IdentifiedModel<String, OrderModel> update(final String domainId, final OrderModel domainModel)
				{
					store.put(domainId, domainModel);
					return IdentifiedModel.of(domainId, domainModel);
				}

				@Override
				public void delete(final String domainId)
				{
					store.remove(domainId);
				}
			};
		}

		@Override
		public AggregateFetchPort<String, OrderModel> fetchPort()
		{
			return new AggregateFetchPort<>()
			{
				@Override
				public Optional<IdentifiedModel<String, OrderModel>> findById(final String domainId)
				{
					return Optional.ofNullable(store.get(domainId)).map(model -> IdentifiedModel.of(domainId, model));
				}

				@Override
				public Collection<IdentifiedModel<String, OrderModel>> findByIds(final Set<String> domainIds)
				{
					return domainIds.stream().flatMap(id -> findById(id).stream()).toList();
				}

				@Override
				public Collection<IdentifiedModel<String, OrderModel>> findAll()
				{
					return store.entrySet().stream()
					            .map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue()))
					            .toList();
				}

				@Override
				public Slice<IdentifiedModel<String, OrderModel>> findAll(final Pageable pageable)
				{
					return new SliceImpl<>(findAll().stream().toList());
				}
			};
		}

		@Override
		public DomainModelBuilder<String, OrderModel> createBuilder()
		{
			return _ -> new OrderModel("CREATED");
		}

		@Override
		public DomainModelPatcher<OrderModel, String> patcher()
		{
			return (_, patch) -> new OrderModel(patch);
		}

		@Override
		public DomainResponseBuilder<OrderModel, String> responseBuilder()
		{
			return OrderModel::status;
		}

		@Override
		public InsertionPolicy<OrderModel> insertionPolicy()
		{
			return _ ->
			{
			};
		}

		@Override
		public PatchPolicy<OrderModel> patchPolicy()
		{
			return (_, _) ->
			{
			};
		}

		@Override
		public DeletionPolicy<OrderModel> deletionPolicy()
		{
			return _ ->
			{
			};
		}

		@Override
		public DomainSecurityPolicy<OrderModel> securityPolicy()
		{
			return DomainSecurityPolicy.allowing();
		}

		@Override
		public DuplicateDefinition<OrderModel> duplicateDefinition()
		{
			return (left, right) -> left.status().equals(right.status());
		}

		@Override
		public PostCommitMutation<String, OrderModel> postCommitMutation()
		{
			return postCommitMutation;
		}

		@Override
		public Collection<AggregateRelationshipDefinitionContract<String, OrderModel, String, String>>
		relationshipDefinitions()
		{
			return List.of();
		}
	}

	private static final class RelationshipAggregateDefinition extends TestAggregateDefinition
	{
		@Override
		public Collection<AggregateRelationshipDefinitionContract<String, OrderModel, String, String>>
		relationshipDefinitions()
		{
			return List.of(new TestRelationshipDefinition());
		}
	}

	private static final class TestRelationshipDefinition
			implements AggregateRelationshipDefinition<String, OrderModel, String, String, Long, Long, Long, Long>
	{
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
		public AggregateCrudDefinition<Long, Long, Long, Long, ?> satelliteDefinition()
		{
			return new SatelliteAggregateDefinition();
		}

		@Override
		public AggregateMutationPort<Long, Long, Long, Long> satelliteMutationPort()
		{
			return new SatelliteMutationPort();
		}

		@Override
		public AggregateFetchPort<Long, Long> satelliteFetchPort()
		{
			return new SatelliteFetchPort();
		}

		@Override
		public SatelliteCreateInputResolver<String, Collection<SatelliteCreateIntent<Long, Long>>> createInputResolver()
		{
			return _ -> List.of();
		}

		@Override
		public SatellitePatchInputResolver<String, Collection<SatelliteMutationIntent<Long, Long, Long>>>
		patchInputResolver()
		{
			return _ -> List.of();
		}

		@Override
		public SatelliteIdentityResolver<OrderModel, Long, Long> identityResolver()
		{
			return (_, _) -> Optional.empty();
		}

		@Override
		public SatelliteLinkStrategy<String, OrderModel, Long, Long> linkStrategy()
		{
			return new SatelliteLinkStrategy<>()
			{
				@Override
				public SatellitePersistenceOrder persistenceOrder()
				{
					return SatellitePersistenceOrder.NO_ORDER_CONSTRAINT;
				}

				@Override
				public Optional<Long> currentLinkedSatelliteDomainId(final OrderModel masterDomainModel)
				{
					return Optional.empty();
				}

				@Override
				public Collection<Long> currentLinkedSatelliteDomainIds(final OrderModel masterDomainModel)
				{
					return List.of();
				}

				@Override
				public OrderModel replaceLinkedSatelliteDomainIds(
						final OrderModel masterDomainModel,
						final Collection<Long> satelliteDomainIds)
				{
					return masterDomainModel;
				}

				@Override
				public OrderModel attachHydratedSatellites(
						final OrderModel masterDomainModel,
						final Collection<IdentifiedModel<Long, Long>> satellites)
				{
					return masterDomainModel;
				}
			};
		}

		@Override
		public SatelliteHydrationStrategy<String, OrderModel, Long, Long> hydrationStrategy()
		{
			return (master, _, _) -> master.model();
		}
	}

	private static final class SatelliteAggregateDefinition
			implements AggregateCrudDefinition<Long, Long, Long, Long, Long>
	{
		@Override
		public AggregateMutationPort<Long, Long, Long, Long> mutationPort()
		{
			return new SatelliteMutationPort();
		}

		@Override
		public AggregateFetchPort<Long, Long> fetchPort()
		{
			return new SatelliteFetchPort();
		}

		@Override
		public DomainModelBuilder<Long, Long> createBuilder()
		{
			return model -> model;
		}

		@Override
		public DomainModelPatcher<Long, Long> patcher()
		{
			return (_, patch) -> patch;
		}

		@Override
		public DomainResponseBuilder<Long, Long> responseBuilder()
		{
			return model -> model;
		}

		@Override
		public InsertionPolicy<Long> insertionPolicy()
		{
			return _ ->
			{
			};
		}

		@Override
		public PatchPolicy<Long> patchPolicy()
		{
			return (_, _) ->
			{
			};
		}

		@Override
		public DeletionPolicy<Long> deletionPolicy()
		{
			return _ ->
			{
			};
		}

		@Override
		public DomainSecurityPolicy<Long> securityPolicy()
		{
			return DomainSecurityPolicy.allowing();
		}

		@Override
		public DuplicateDefinition<Long> duplicateDefinition()
		{
			return Long::equals;
		}

		@Override
		public PostCommitMutation<Long, Long> postCommitMutation()
		{
			return PostCommitMutation.noop();
		}

		@Override
		public Collection<AggregateRelationshipDefinitionContract<Long, Long, Long, Long>> relationshipDefinitions()
		{
			return List.of();
		}
	}

	private static final class SatelliteMutationPort implements AggregateMutationPort<Long, Long, Long, Long>
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

	private static final class SatelliteFetchPort implements AggregateFetchPort<Long, Long>
	{
		@Override
		public Optional<IdentifiedModel<Long, Long>> findById(final Long domainId)
		{
			return Optional.of(IdentifiedModel.of(domainId, domainId));
		}

		@Override
		public Collection<IdentifiedModel<Long, Long>> findByIds(final Set<Long> domainIds)
		{
			return domainIds.stream().map(id -> IdentifiedModel.of(id, id)).toList();
		}

		@Override
		public Collection<IdentifiedModel<Long, Long>> findAll()
		{
			return List.of();
		}

		@Override
		public Slice<IdentifiedModel<Long, Long>> findAll(final Pageable pageable)
		{
			return new SliceImpl<>(List.of());
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

	private record RecordingDurableProcessStarter(List<DurableProcessStartRequest<?, ?>> startedRequests)
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