package de.gupta.clean.crud.template.useCases.operationOLD.creation.aggregate.service;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutation;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteCreateIntent;
import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteMutationIntent;
import de.gupta.clean.crud.template.domain.aggregate.lifecycle.AggregateLifecycle;
import de.gupta.clean.crud.template.domain.aggregate.lifecycle.DefaultAggregateLifecycle;
import de.gupta.clean.crud.template.domain.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.domain.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.domain.aggregate.relationship.*;
import de.gupta.clean.crud.template.domain.mapping.fetch.DomainResponseBuilder;
import de.gupta.clean.crud.template.domain.mapping.save.DomainModelBuilder;
import de.gupta.clean.crud.template.domain.mapping.update.DomainModelPatcher;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.handler.CreationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.handler.RegisteredCreationHandler;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.model.CreationContext;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.model.CreationRequest;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.plan.AggregateCreationPlan;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.invariant.CreationInvariantPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.profile.CreationPolicyProfile;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.profile.CreationPolicyProfileResolver;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.quarantine.application.recording.CreationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.invariant.InvariantViolation;
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

import static org.junit.jupiter.api.Assertions.*;

class AggregateCreationServicesTest
{
	@Test
	void creationServiceCreatesAggregateFromRegisteredHandler()
	{
		var definition = new TestAggregateDefinition();
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = creationService("test-aggregate", definition, engine);

		var created = service.create(new CreationRequest<>(
				new OpenOrder("AAPL", 100),
				OperationSource.INTERNAL_COMMAND)).createdOrThrow();

		assertEquals("order-1", created.domainId());
		assertEquals("AAPL:100", created.model().status());
		assertEquals("AAPL:100", definition.store.get("order-1").status());
	}

	private static DefaultAggregateCreationService<String, OrderModel, OrderCreate, String, String> creationService(
			final String aggregateKey,
			final AggregateDefinition<String, OrderModel, OrderCreate, String, String> definition,
			final AggregateLifecycle engine)
	{
		return (DefaultAggregateCreationService<String, OrderModel, OrderCreate, String, String>)
				AggregateCreationServices.creationService(
						aggregateKey,
						definition,
						engine,
						registry(),
						CreationQuarantineRecorder.noop());
	}

	private static CreationHandlerRegistry<OrderCreate> registry()
	{
		return CreationHandlerRegistry.of(List.of(
				RegisteredCreationHandler.of(OpenOrder.class,
						payload -> AggregateCreationPlan.rootOnly(
								new OrderCreate(payload.symbol(), payload.quantity())))));
	}

	@Test
	void creationServiceDispatchesCreateShapedPostCommitMutation()
			throws InterruptedException
	{
		var definition = new TestAggregateDefinition();
		var contexts = Collections.synchronizedList(new ArrayList<PostCommitMutationContext<String, OrderModel>>());
		var latch = new CountDownLatch(1);
		definition.postCommitMutation = context ->
		{
			contexts.add(context);
			latch.countDown();
		};
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = creationService("test-aggregate", definition, engine);

		service.create(new CreationRequest<>(new OpenOrder("AAPL", 100), OperationSource.INTERNAL_COMMAND));

		assertTrue(latch.await(2, TimeUnit.SECONDS));
		assertEquals(1, contexts.size());
		assertEquals(PostCommitMutationKind.CREATE, contexts.getFirst().kind());
		assertEquals("order-1", contexts.getFirst().domainId());
		assertEquals("AAPL:100", contexts.getFirst().currentModel().orElseThrow().status());
	}

	@Test
	void creationServiceCanStartDurableProcessesUsingCreationContext()
	{
		var definition = new TestAggregateDefinition();
		var startedRequests = new ArrayList<DurableProcessStartRequest<?, ?>>();
		var engine = DefaultAggregateLifecycle.withTransactionRunnerAndDurableProcessStarter(
				new InlineTransactionRunner(),
				new RecordingDurableProcessStarter(startedRequests));
		var processDefinition = DurableProcessDefinition.of("order-create-follow-up", OrderCreated.class,
				OrderPayload.class);
		var retryPolicy = new RetryPolicy(3, BackoffPolicy.fixed(Duration.ofSeconds(1)));
		var service = AggregateCreationServices.creationService(
				"test-aggregate",
				definition,
				engine,
				registry(),
				context -> List.of(new DurableProcessStartRequest<>(
						processDefinition,
						new OrderCreated(context.domainId().orElseThrow()),
						new OrderPayload(context.afterModel().orElseThrow().status()),
						new CorrelationId("creation:" + context.domainId().orElseThrow()),
						retryPolicy)),
				CreationQuarantineRecorder.noop());

		service.create(new CreationRequest<>(new OpenOrder("AAPL", 100), OperationSource.INTERNAL_COMMAND));

		assertEquals(1, startedRequests.size());
		assertEquals("order-create-follow-up", startedRequests.getFirst().definition().processType());
	}

	@Test
	void creationServicePassesCorrelationAndCausationMetadataToDurableProcessMapping()
	{
		var definition = new TestAggregateDefinition();
		var observedContexts = new ArrayList<CreationContext<String, OrderModel>>();
		var engine = DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = AggregateCreationServices.creationService(
				"test-aggregate",
				definition,
				engine,
				registry(),
				context ->
				{
					observedContexts.add(context);
					return List.of();
				},
				CreationQuarantineRecorder.noop());

		service.create(new CreationRequest<>(
				new OpenOrder("AAPL", 100),
				OperationSource.PROCESS_EMITTED_ACTION,
				Optional.of(
						new de.gupta.clean.crud.template.useCases.operationOLD.domain.model.id.OperationCorrelationId(
								"corr-1")),
				Optional.of(
						new de.gupta.clean.crud.template.useCases.operationOLD.domain.model.id.OperationCausationId(
								"cause-1"))));

		assertEquals(1, observedContexts.size());
		assertEquals(OperationFamily.APPLICATION, observedContexts.getFirst().family());
		assertEquals(OpenOrder.class, observedContexts.getFirst().payloadType());
		assertEquals("corr-1", observedContexts.getFirst().correlationId().orElseThrow().value());
		assertEquals("cause-1", observedContexts.getFirst().causationId().orElseThrow().value());
	}

	@Test
	void creationServiceRejectsUnknownPayloadType()
	{
		var definition = new TestAggregateDefinition();
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = AggregateCreationServices.creationService("test-aggregate", definition, engine,
				CreationHandlerRegistry.of(List.of()),
				CreationQuarantineRecorder.noop());

		var exception = assertThrows(
				InvalidRequestException.class,
				() -> service.create(new CreationRequest<>(
						new OpenOrder("AAPL", 100),
						OperationSource.AUTHORITATIVE_EXTERNAL_EVENT)));

		assertTrue(exception.getMessage().contains(OpenOrder.class.getName()));
	}

	@Test
	void userIntentCreationsStillRespectAccessPolicy()
	{
		var definition = new AccessDeniedAggregateDefinition();
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = creationService("test-aggregate", definition, engine);

		assertThrows(
				AccessDeniedException.class,
				() -> service.create(new CreationRequest<>(
						new OpenOrder("AAPL", 100),
						OperationSource.USER_INTENT)));
	}

	@Test
	void authoritativeExternalEventsBypassAccessPolicyButStillCreate()
	{
		var definition = new AccessDeniedAggregateDefinition();
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = creationService("test-aggregate", definition, engine);

		var created = service.create(new CreationRequest<>(
				new OpenOrder("AAPL", 100),
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT)).createdOrThrow();

		assertEquals("AAPL:100", created.model().status());
	}

	@Test
	void authoritativeExternalEventCanBeQuarantined()
	{
		var definition = new QuarantiningAggregateDefinition();
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = creationService("test-aggregate", definition, engine);

		var result = service.createWithResult(new CreationRequest<>(
				new OpenOrder("AAPL", 100),
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT));

		assertTrue(result.quarantined());
		assertTrue(result.created().isEmpty());
		assertEquals("External order source unavailable",
				result.quarantineRequest().orElseThrow().violations().getFirst().message());
	}

	@Test
	void creationServicePersistsOwnedInlineSatellites()
	{
		var definition = new AggregateOrderDefinition();
		var engine = DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service =
				AggregateCreationServices.creationService(
						"test-aggregate",
						definition,
						engine,
						aggregateRegistry(),
						CreationQuarantineRecorder.noop());

		var created = service.create(new CreationRequest<>(
				new OpenOrderWithLine("AAPL", "entry"),
				OperationSource.INTERNAL_COMMAND)).createdOrThrow();

		assertEquals("order-1", created.domainId());
		assertEquals(List.of(1L), created.model().lineIds());
		assertEquals("entry", definition.lineStore.get(1L).value());
	}

	private static CreationHandlerRegistry<AggregateOrderCreate> aggregateRegistry()
	{
		return CreationHandlerRegistry.of(List.of(
				RegisteredCreationHandler.of(OpenOrderWithLine.class,
						payload -> AggregateCreationPlan.rootOnly(new AggregateOrderCreate(
								payload.symbol(),
								List.of(new SatelliteCreateIntent.InlineSatelliteCreateIntent<>(
										new OrderLineCreate(payload.lineValue()))))))));
	}

	@Test
	void creationServiceCanQuarantineOwnedInlineSatelliteCreation()
	{
		var definition = new QuarantiningAggregateOrderDefinition();
		var engine = DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service =
				AggregateCreationServices.creationService(
						"test-aggregate",
						definition,
						engine,
						aggregateRegistry(),
						CreationQuarantineRecorder.noop());

		var result = service.createWithResult(new CreationRequest<>(
				new OpenOrderWithLine("AAPL", "blocked"),
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT));

		assertTrue(result.quarantined());
		assertTrue(definition.store.isEmpty());
		assertTrue(definition.lineStore.isEmpty());
		assertEquals("External line source unavailable",
				result.quarantineRequest().orElseThrow().violations().getFirst().message());
	}

	private record OpenOrder(String symbol, int quantity) implements ApplicationOperationPayload
	{
	}

	private record OrderCreate(String symbol, int quantity)
	{
	}

	private record OrderModel(String status)
	{
	}

	private record OrderCreated(String orderId) implements DurableProcessTrigger
	{
	}

	private record OrderPayload(String status) implements DurableProcessPayload
	{
	}

	private record OpenOrderWithLine(String symbol, String lineValue) implements ApplicationOperationPayload
	{
	}

	private record AggregateOrderCreate(
			String symbol,
			Collection<SatelliteCreateIntent<Long, OrderLineCreate>> lineCreateIntents)
	{
	}

	private record AggregateOrderModel(String status, List<Long> lineIds)
	{
		private AggregateOrderModel withLineIds(final Collection<Long> newLineIds)
		{
			return new AggregateOrderModel(status, List.copyOf(newLineIds));
		}
	}

	private record OrderLineCreate(String value)
	{
	}

	private record OrderLinePatch(String value)
	{
	}

	private record OrderLineModel(String value)
	{
	}

	private static class TestAggregateDefinition
			implements AggregateDefinition<String, OrderModel, OrderCreate, String, String>
	{
		protected final Map<String, OrderModel> store = new LinkedHashMap<>();
		protected PostCommitMutation<String, OrderModel> postCommitMutation = _ ->
		{
		};
		private int nextId = 1;

		@Override
		public AggregateMutationPort<String, OrderModel, OrderCreate, String> mutationPort()
		{
			return new AggregateMutationPort<>()
			{
				@Override
				public IdentifiedModel<String, OrderModel> create(final OrderModel model)
				{
					var id = "order-" + nextId++;
					store.put(id, model);
					return IdentifiedModel.of(id, model);
				}

				@Override
				public void put(final String id, final OrderModel model)
				{
					store.put(id, model);
				}

				@Override
				public IdentifiedModel<String, OrderModel> update(final String id, final OrderModel replacement)
				{
					store.put(id, replacement);
					return IdentifiedModel.of(id, replacement);
				}

				@Override
				public void delete(final String id)
				{
					store.remove(id);
				}
			};
		}

		@Override
		public AggregateFetchPort<String, OrderModel> fetchPort()
		{
			return new AggregateFetchPort<>()
			{
				@Override
				public Optional<IdentifiedModel<String, OrderModel>> findById(final String id)
				{
					return Optional.ofNullable(store.get(id)).map(model -> IdentifiedModel.of(id, model));
				}

				@Override
				public Collection<IdentifiedModel<String, OrderModel>> findByIds(final Set<String> ids)
				{
					return ids.stream()
					          .map(this::findById)
					          .flatMap(Optional::stream)
					          .toList();
				}

				@Override
				public Collection<IdentifiedModel<String, OrderModel>> findAll()
				{
					return store.entrySet().stream().map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue()))
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
		public DomainModelBuilder<OrderCreate, OrderModel> createBuilder()
		{
			return create -> new OrderModel(create.symbol() + ":" + create.quantity());
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
		public DeletionPolicy<OrderModel> deletionPolicy()
		{
			return _ ->
			{
			};
		}

		@Override
		public DuplicateDefinition<OrderModel> duplicateDefinition()
		{
			return OrderModel::equals;
		}

		@Override
		public PostCommitMutation<String, OrderModel> postCommitMutation()
		{
			return postCommitMutation;
		}

		@Override
		public Collection<AggregateRelationshipDefinitionContract<String, OrderModel, OrderCreate, String>>
		relationshipDefinitions()
		{
			return List.of();
		}

		@Override
		public DomainSecurityPolicy<OrderModel> securityPolicy()
		{
			return _ -> true;
		}

		@Override
		public PatchPolicy<OrderModel> patchPolicy()
		{
			return (_, _) ->
			{
			};
		}

		@Override
		public InsertionPolicy<OrderModel> insertionPolicy()
		{
			return _ ->
			{
			};
		}
	}

	private static final class AccessDeniedAggregateDefinition extends TestAggregateDefinition
	{
		@Override
		public DomainSecurityPolicy<OrderModel> securityPolicy()
		{
			return _ -> false;
		}

		@Override
		public de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.profile.CreationPolicyProfileResolver creationPolicyProfileResolver()
		{
			return source -> switch (source)
			{
				case AUTHORITATIVE_EXTERNAL_EVENT -> CreationPolicyProfile.authoritativeExternalEvent();
				default -> CreationPolicyProfileResolver.defaultResolver().resolve(source);
			};
		}
	}

	private static final class QuarantiningAggregateDefinition extends TestAggregateDefinition
	{
		@Override
		public CreationPolicyProfileResolver creationPolicyProfileResolver()
		{
			return source -> source == OperationSource.AUTHORITATIVE_EXTERNAL_EVENT
					? CreationPolicyProfile.authoritativeExternalEvent()
					: CreationPolicyProfileResolver.defaultResolver().resolve(source);
		}

		@Override
		public CreationInvariantPolicy<OrderModel> creationInvariantPolicy()
		{
			return (source, afterModel) -> source == OperationSource.AUTHORITATIVE_EXTERNAL_EVENT
					? List.of(InvariantViolation.hard("External order source unavailable"))
					: List.of();
		}
	}

	private static final class InlineTransactionRunner implements PersistenceTransactionRunner
	{
		@Override
		public <T> T inTransaction(final java.util.function.Supplier<T> body)
		{
			return body.get();
		}

		@Override
		public void inTransaction(final Runnable body)
		{
			body.run();
		}
	}

	private record RecordingDurableProcessStarter(List<DurableProcessStartRequest<?, ?>> startedRequests)
			implements DurableProcessStarter
	{
		@Override
		public DurableProcessTaskId start(final DurableProcessStartRequest<?, ?> startRequest)
		{
			startedRequests.add(startRequest);
			return new DurableProcessTaskId("task-1");
		}

	}

	private static class AggregateOrderDefinition
			implements AggregateDefinition<String, AggregateOrderModel, AggregateOrderCreate, String, String>
	{
		protected final Map<String, AggregateOrderModel> store = new LinkedHashMap<>();
		protected final Map<Long, OrderLineModel> lineStore = new LinkedHashMap<>();
		private int nextId = 1;
		private long nextLineId = 1L;

		@Override
		public AggregateMutationPort<String, AggregateOrderModel, AggregateOrderCreate, String> mutationPort()
		{
			return new AggregateMutationPort<>()
			{
				@Override
				public IdentifiedModel<String, AggregateOrderModel> create(final AggregateOrderModel model)
				{
					var id = "order-" + nextId++;
					store.put(id, model);
					return IdentifiedModel.of(id, model);
				}

				@Override
				public void put(final String id, final AggregateOrderModel model)
				{
					store.put(id, model);
				}

				@Override
				public IdentifiedModel<String, AggregateOrderModel> update(
						final String id,
						final AggregateOrderModel replacement)
				{
					store.put(id, replacement);
					return IdentifiedModel.of(id, replacement);
				}

				@Override
				public void delete(final String id)
				{
					store.remove(id);
				}
			};
		}

		@Override
		public AggregateFetchPort<String, AggregateOrderModel> fetchPort()
		{
			return new AggregateFetchPort<>()
			{
				@Override
				public Optional<IdentifiedModel<String, AggregateOrderModel>> findById(final String id)
				{
					return Optional.ofNullable(store.get(id)).map(model -> IdentifiedModel.of(id, model));
				}

				@Override
				public Collection<IdentifiedModel<String, AggregateOrderModel>> findByIds(final Set<String> ids)
				{
					return ids.stream().map(this::findById).flatMap(Optional::stream).toList();
				}

				@Override
				public Collection<IdentifiedModel<String, AggregateOrderModel>> findAll()
				{
					return store.entrySet().stream().map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue()))
					            .toList();
				}

				@Override
				public Slice<IdentifiedModel<String, AggregateOrderModel>> findAll(final Pageable pageable)
				{
					return new SliceImpl<>(findAll().stream().toList());
				}
			};
		}

		@Override
		public DomainModelBuilder<AggregateOrderCreate, AggregateOrderModel> createBuilder()
		{
			return create -> new AggregateOrderModel(create.symbol(), List.of());
		}

		@Override
		public DomainModelPatcher<AggregateOrderModel, String> patcher()
		{
			return (_, patch) -> new AggregateOrderModel(patch, List.of());
		}

		@Override
		public DomainResponseBuilder<AggregateOrderModel, String> responseBuilder()
		{
			return AggregateOrderModel::status;
		}

		@Override
		public DeletionPolicy<AggregateOrderModel> deletionPolicy()
		{
			return _ ->
			{
			};
		}

		@Override
		public DuplicateDefinition<AggregateOrderModel> duplicateDefinition()
		{
			return AggregateOrderModel::equals;
		}

		@Override
		public PostCommitMutation<String, AggregateOrderModel> postCommitMutation()
		{
			return _ ->
			{
			};
		}

		@Override
		public Collection<AggregateRelationshipDefinitionContract<String, AggregateOrderModel, AggregateOrderCreate, String>>
		relationshipDefinitions()
		{
			return List.of(new AggregateOrderLineRelationshipDefinition());
		}

		@Override
		public DomainSecurityPolicy<AggregateOrderModel> securityPolicy()
		{
			return _ -> true;
		}

		@Override
		public PatchPolicy<AggregateOrderModel> patchPolicy()
		{
			return (_, _) ->
			{
			};
		}

		@Override
		public InsertionPolicy<AggregateOrderModel> insertionPolicy()
		{
			return _ ->
			{
			};
		}

		protected AggregateDefinition<Long, OrderLineModel, OrderLineCreate, OrderLinePatch, String> lineDefinition()
		{
			return new AggregateDefinition<>()
			{
				@Override
				public AggregateMutationPort<Long, OrderLineModel, OrderLineCreate, OrderLinePatch> mutationPort()
				{
					return new AggregateMutationPort<>()
					{
						@Override
						public IdentifiedModel<Long, OrderLineModel> create(final OrderLineModel model)
						{
							var id = nextLineId++;
							lineStore.put(id, model);
							return IdentifiedModel.of(id, model);
						}

						@Override
						public void put(final Long id, final OrderLineModel model)
						{
							lineStore.put(id, model);
						}

						@Override
						public IdentifiedModel<Long, OrderLineModel> update(final Long id,
						                                                    final OrderLineModel replacement)
						{
							lineStore.put(id, replacement);
							return IdentifiedModel.of(id, replacement);
						}

						@Override
						public void delete(final Long id)
						{
							lineStore.remove(id);
						}
					};
				}

				@Override
				public AggregateFetchPort<Long, OrderLineModel> fetchPort()
				{
					return new AggregateFetchPort<>()
					{
						@Override
						public Optional<IdentifiedModel<Long, OrderLineModel>> findById(final Long id)
						{
							return Optional.ofNullable(lineStore.get(id)).map(model -> IdentifiedModel.of(id, model));
						}

						@Override
						public Collection<IdentifiedModel<Long, OrderLineModel>> findByIds(final Set<Long> ids)
						{
							return ids.stream().map(this::findById).flatMap(Optional::stream).toList();
						}

						@Override
						public Collection<IdentifiedModel<Long, OrderLineModel>> findAll()
						{
							return lineStore.entrySet().stream()
							                .map(entry -> IdentifiedModel.of(entry.getKey(), entry.getValue()))
							                .toList();
						}

						@Override
						public Slice<IdentifiedModel<Long, OrderLineModel>> findAll(final Pageable pageable)
						{
							return new SliceImpl<>(findAll().stream().toList());
						}
					};
				}

				@Override
				public DomainModelBuilder<OrderLineCreate, OrderLineModel> createBuilder()
				{
					return create -> new OrderLineModel(create.value());
				}

				@Override
				public DomainModelPatcher<OrderLineModel, OrderLinePatch> patcher()
				{
					return (_, patch) -> new OrderLineModel(patch.value());
				}

				@Override
				public DomainResponseBuilder<OrderLineModel, String> responseBuilder()
				{
					return OrderLineModel::value;
				}

				@Override
				public DeletionPolicy<OrderLineModel> deletionPolicy()
				{
					return _ ->
					{
					};
				}

				@Override
				public DuplicateDefinition<OrderLineModel> duplicateDefinition()
				{
					return OrderLineModel::equals;
				}

				@Override
				public PostCommitMutation<Long, OrderLineModel> postCommitMutation()
				{
					return _ ->
					{
					};
				}

				@Override
				public Collection<AggregateRelationshipDefinitionContract<Long, OrderLineModel, OrderLineCreate, OrderLinePatch>>
				relationshipDefinitions()
				{
					return List.of();
				}

				@Override
				public DomainSecurityPolicy<OrderLineModel> securityPolicy()
				{
					return _ -> true;
				}

				@Override
				public PatchPolicy<OrderLineModel> patchPolicy()
				{
					return (_, _) ->
					{
					};
				}

				@Override
				public InsertionPolicy<OrderLineModel> insertionPolicy()
				{
					return _ ->
					{
					};
				}
			};
		}

		protected class AggregateOrderLineRelationshipDefinition
				implements
				AggregateRelationshipDefinition<String, AggregateOrderModel, AggregateOrderCreate, String, Long,
						OrderLineModel, OrderLineCreate, OrderLinePatch>
		{
			@Override
			public String name()
			{
				return "lines";
			}

			@Override
			public Cardinality cardinality()
			{
				return Cardinality.MANY;
			}

			@Override
			public de.gupta.clean.crud.template.domain.relationship.RelationshipKind relationshipKind()
			{
				return de.gupta.clean.crud.template.domain.relationship.RelationshipKind.OWNED;
			}

			@Override
			public de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics lifecycleSemantics()
			{
				return de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics.of(true, true, true, true,
						true);
			}

			@Override
			public de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy reconciliationStrategy()
			{
				return de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy.REPLACE;
			}

			@Override
			public AggregateDefinition<Long, OrderLineModel, OrderLineCreate, OrderLinePatch, ?> satelliteDefinition()
			{
				return lineDefinition();
			}

			@Override
			public AggregateMutationPort<Long, OrderLineModel, OrderLineCreate, OrderLinePatch> satelliteMutationPort()
			{
				return lineDefinition().mutationPort();
			}

			@Override
			public AggregateFetchPort<Long, OrderLineModel> satelliteFetchPort()
			{
				return lineDefinition().fetchPort();
			}

			@Override
			public SatelliteCreateInputResolver<AggregateOrderCreate, Collection<SatelliteCreateIntent<Long, OrderLineCreate>>>
			createInputResolver()
			{
				return AggregateOrderCreate::lineCreateIntents;
			}

			@Override
			public SatellitePatchInputResolver<String, Collection<SatelliteMutationIntent<Long, OrderLineCreate, OrderLinePatch>>>
			patchInputResolver()
			{
				return _ -> List.of();
			}

			@Override
			public SatelliteIdentityResolver<AggregateOrderModel, OrderLineModel, Long> identityResolver()
			{
				return (_, _) -> Optional.empty();
			}

			@Override
			public SatelliteLinkStrategy<String, AggregateOrderModel, Long, OrderLineModel> linkStrategy()
			{
				return new SatelliteLinkStrategy<>()
				{
					@Override
					public SatellitePersistenceOrder persistenceOrder()
					{
						return SatellitePersistenceOrder.SATELLITE_BEFORE_MASTER;
					}

					@Override
					public Optional<Long> currentLinkedSatelliteDomainId(final AggregateOrderModel masterDomainModel)
					{
						return masterDomainModel.lineIds().stream().findFirst();
					}

					@Override
					public Collection<Long> currentLinkedSatelliteDomainIds(final AggregateOrderModel masterDomainModel)
					{
						return masterDomainModel.lineIds();
					}

					@Override
					public AggregateOrderModel replaceLinkedSatelliteDomainIds(
							final AggregateOrderModel masterDomainModel,
							final Collection<Long> satelliteDomainIds)
					{
						return masterDomainModel.withLineIds(satelliteDomainIds);
					}

					@Override
					public AggregateOrderModel attachHydratedSatellites(
							final AggregateOrderModel masterDomainModel,
							final Collection<IdentifiedModel<Long, OrderLineModel>> satellites)
					{
						return masterDomainModel;
					}
				};
			}

			@Override
			public SatelliteHydrationStrategy<String, AggregateOrderModel, Long, OrderLineModel> hydrationStrategy()
			{
				return (master, satelliteFetchPort, satelliteLinkStrategy) -> master.model();
			}
		}
	}

	private static final class QuarantiningAggregateOrderDefinition extends AggregateOrderDefinition
	{
		@Override
		public CreationPolicyProfileResolver creationPolicyProfileResolver()
		{
			return source -> source == OperationSource.AUTHORITATIVE_EXTERNAL_EVENT
					? CreationPolicyProfile.authoritativeExternalEvent()
					: CreationPolicyProfileResolver.defaultResolver().resolve(source);
		}

		@Override
		public de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.invariant.CreationInvariantPolicy<AggregateOrderModel>
		creationInvariantPolicy()
		{
			return (_, _) -> List.of();
		}

		@Override
		public Collection<AggregateRelationshipDefinitionContract<String, AggregateOrderModel, AggregateOrderCreate, String>>
		relationshipDefinitions()
		{
			return List.of(new QuarantiningAggregateOrderLineRelationshipDefinition());
		}

		private AggregateDefinition<Long, OrderLineModel, OrderLineCreate, OrderLinePatch, String>
		quarantiningLineDefinition()
		{
			var base = lineDefinition();
			return new AggregateDefinition<>()
			{
				@Override
				public AggregateMutationPort<Long, OrderLineModel, OrderLineCreate, OrderLinePatch> mutationPort()
				{
					return base.mutationPort();
				}

				@Override
				public AggregateFetchPort<Long, OrderLineModel> fetchPort()
				{
					return base.fetchPort();
				}

				@Override
				public DomainModelBuilder<OrderLineCreate, OrderLineModel> createBuilder()
				{
					return base.createBuilder();
				}

				@Override
				public DomainModelPatcher<OrderLineModel, OrderLinePatch> patcher()
				{
					return base.patcher();
				}

				@Override
				public DomainResponseBuilder<OrderLineModel, String> responseBuilder()
				{
					return base.responseBuilder();
				}

				@Override
				public DeletionPolicy<OrderLineModel> deletionPolicy()
				{
					return base.deletionPolicy();
				}

				@Override
				public DuplicateDefinition<OrderLineModel> duplicateDefinition()
				{
					return base.duplicateDefinition();
				}

				@Override
				public PostCommitMutation<Long, OrderLineModel> postCommitMutation()
				{
					return base.postCommitMutation();
				}

				@Override
				public Collection<AggregateRelationshipDefinitionContract<Long, OrderLineModel, OrderLineCreate, OrderLinePatch>>
				relationshipDefinitions()
				{
					return base.relationshipDefinitions();
				}

				@Override
				public DomainSecurityPolicy<OrderLineModel> securityPolicy()
				{
					return base.securityPolicy();
				}

				@Override
				public PatchPolicy<OrderLineModel> patchPolicy()
				{
					return base.patchPolicy();
				}

				@Override
				public CreationPolicyProfileResolver creationPolicyProfileResolver()
				{
					return source -> source == OperationSource.AUTHORITATIVE_EXTERNAL_EVENT
							? CreationPolicyProfile.authoritativeExternalEvent()
							: CreationPolicyProfileResolver.defaultResolver().resolve(source);
				}

				@Override
				public InsertionPolicy<OrderLineModel> insertionPolicy()
				{
					return base.insertionPolicy();
				}

				@Override
				public CreationInvariantPolicy<OrderLineModel> creationInvariantPolicy()
				{
					return (source, afterModel) -> source == OperationSource.AUTHORITATIVE_EXTERNAL_EVENT
							&& afterModel.value().equals("blocked")
							? List.of(InvariantViolation.hard("External line source unavailable"))
							: List.of();
				}
			};
		}

		private final class QuarantiningAggregateOrderLineRelationshipDefinition
				extends AggregateOrderLineRelationshipDefinition
		{
			@Override
			public AggregateDefinition<Long, OrderLineModel, OrderLineCreate, OrderLinePatch, ?> satelliteDefinition()
			{
				return quarantiningLineDefinition();
			}
		}
	}
}
