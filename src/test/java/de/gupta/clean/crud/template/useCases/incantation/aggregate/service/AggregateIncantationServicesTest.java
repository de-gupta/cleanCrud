package de.gupta.clean.crud.template.useCases.incantation.aggregate.service;

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
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutation;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.AggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.DefaultAggregateLifecycleEngine;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateFetchPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.port.AggregateMutationPort;
import de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinitionContract;
import de.gupta.clean.crud.template.useCases.incantation.domain.handler.IncantationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.incantation.domain.handler.RegisteredIncantationHandler;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.*;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.invariant.IncantationInvariantPolicy;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.profile.IncantationPolicyProfile;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.profile.IncantationPolicyProfileResolver;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant.InvariantViolation;
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

class AggregateIncantationServicesTest
{
	@Test
	void incantationServiceCreatesAggregateFromRegisteredHandler()
	{
		var definition = new TestAggregateDefinition();
		AggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		var service = incantationService(definition, engine);

		var created = service.incant(new IncantationRequest<>(
				new OpenOrder("AAPL", 100),
				IncantationSource.INTERNAL_COMMAND));

		assertEquals("order-1", created.domainId());
		assertEquals("AAPL:100", created.model().status());
		assertEquals("AAPL:100", definition.store.get("order-1").status());
	}

	@Test
	void incantationServiceDispatchesCreateShapedPostCommitMutation()
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
		AggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		var service = incantationService(definition, engine);

		service.incant(new IncantationRequest<>(new OpenOrder("AAPL", 100), IncantationSource.INTERNAL_COMMAND));

		assertTrue(latch.await(2, TimeUnit.SECONDS));
		assertEquals(1, contexts.size());
		assertEquals(PostCommitMutationKind.CREATE, contexts.getFirst().kind());
		assertEquals("order-1", contexts.getFirst().domainId());
		assertEquals("AAPL:100", contexts.getFirst().currentModel().orElseThrow().status());
	}

	@Test
	void incantationServiceCanStartDurableProcessesUsingIncantationContext()
	{
		var definition = new TestAggregateDefinition();
		var startedRequests = new ArrayList<DurableProcessStartRequest<?, ?>>();
		var engine = DefaultAggregateLifecycleEngine.withTransactionRunnerAndDurableProcessStarter(
				new InlineTransactionRunner(),
				new RecordingDurableProcessStarter(startedRequests));
		var processDefinition = DurableProcessDefinition.of("order-create-follow-up", OrderCreated.class,
				OrderPayload.class);
		var retryPolicy = new RetryPolicy(3, BackoffPolicy.fixed(Duration.ofSeconds(1)));
		var service = AggregateIncantationServices.incantationService(
				definition,
				engine,
				registry(),
				context -> List.of(new DurableProcessStartRequest<>(
						processDefinition,
						new OrderCreated(context.domainId().orElseThrow()),
						new OrderPayload(context.afterModel().orElseThrow().status()),
						new CorrelationId("incantation:" + context.domainId().orElseThrow()),
						retryPolicy)));

		service.incant(new IncantationRequest<>(new OpenOrder("AAPL", 100), IncantationSource.INTERNAL_COMMAND));

		assertEquals(1, startedRequests.size());
		assertEquals("order-create-follow-up", startedRequests.getFirst().definition().processType());
	}

	@Test
	void incantationServicePassesCorrelationAndCausationMetadataToDurableProcessMapping()
	{
		var definition = new TestAggregateDefinition();
		var observedContexts = new ArrayList<IncantationContext<String, OrderModel>>();
		var engine = DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		var service = AggregateIncantationServices.incantationService(
				definition,
				engine,
				registry(),
				context ->
				{
					observedContexts.add(context);
					return List.of();
				});

		service.incant(new IncantationRequest<>(
				new OpenOrder("AAPL", 100),
				IncantationSource.PROCESS_EMITTED_ACTION,
				Optional.of(
						new de.gupta.clean.crud.template.useCases.incantation.domain.model.id.IncantationCorrelationId(
								"corr-1")),
				Optional.of(
						new de.gupta.clean.crud.template.useCases.incantation.domain.model.id.IncantationCausationId(
								"cause-1"))));

		assertEquals(1, observedContexts.size());
		assertEquals(IncantationFamily.APPLICATION, observedContexts.getFirst().family());
		assertEquals(OpenOrder.class, observedContexts.getFirst().payloadType());
		assertEquals("corr-1", observedContexts.getFirst().correlationId().orElseThrow().value());
		assertEquals("cause-1", observedContexts.getFirst().causationId().orElseThrow().value());
	}

	@Test
	void incantationServiceRejectsUnknownPayloadType()
	{
		var definition = new TestAggregateDefinition();
		AggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		var service = AggregateIncantationServices.incantationService(definition, engine,
				IncantationHandlerRegistry.of(List.of()));

		var exception = assertThrows(
				InvalidRequestException.class,
				() -> service.incant(new IncantationRequest<>(
						new OpenOrder("AAPL", 100),
						IncantationSource.AUTHORITATIVE_EXTERNAL_EVENT)));

		assertTrue(exception.getMessage().contains(OpenOrder.class.getName()));
	}

	@Test
	void userIntentIncantationsStillRespectAccessPolicy()
	{
		var definition = new AccessDeniedAggregateDefinition();
		AggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		var service = incantationService(definition, engine);

		assertThrows(
				AccessDeniedException.class,
				() -> service.incant(new IncantationRequest<>(
						new OpenOrder("AAPL", 100),
						IncantationSource.USER_INTENT)));
	}

	@Test
	void authoritativeExternalEventsBypassAccessPolicyButStillCreate()
	{
		var definition = new AccessDeniedAggregateDefinition();
		AggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		var service = incantationService(definition, engine);

		var created = service.incant(new IncantationRequest<>(
				new OpenOrder("AAPL", 100),
				IncantationSource.AUTHORITATIVE_EXTERNAL_EVENT));

		assertEquals("AAPL:100", created.model().status());
	}

	@Test
	void authoritativeExternalEventCanBeQuarantined()
	{
		var definition = new QuarantiningAggregateDefinition();
		AggregateLifecycleEngine engine =
				DefaultAggregateLifecycleEngine.withTransactionRunner(new InlineTransactionRunner());
		var service = incantationService(definition, engine);

		var result = service.incantWithResult(new IncantationRequest<>(
				new OpenOrder("AAPL", 100),
				IncantationSource.AUTHORITATIVE_EXTERNAL_EVENT));

		assertTrue(result.quarantined());
		assertTrue(result.created().isEmpty());
		assertEquals("External order source unavailable",
				result.quarantineRequest().orElseThrow().violations().getFirst().message());
	}

	private static DefaultAggregateIncantationService<String, OrderModel, OrderCreate, String, String> incantationService(
			final AggregateCrudDefinition<String, OrderModel, OrderCreate, String, String> definition,
			final AggregateLifecycleEngine engine)
	{
		return (DefaultAggregateIncantationService<String, OrderModel, OrderCreate, String, String>)
				AggregateIncantationServices.incantationService(definition, engine, registry());
	}

	private static IncantationHandlerRegistry<OrderCreate> registry()
	{
		return IncantationHandlerRegistry.of(List.of(
				RegisteredIncantationHandler.of(OpenOrder.class,
						payload -> new OrderCreate(payload.symbol(), payload.quantity()))));
	}

	private record OpenOrder(String symbol, int quantity) implements ApplicationIncantationPayload
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

	private static class TestAggregateDefinition
			implements AggregateCrudDefinition<String, OrderModel, OrderCreate, String, String>
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
			return _ -> true;
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
	}

	private static final class AccessDeniedAggregateDefinition extends TestAggregateDefinition
	{
		@Override
		public DomainSecurityPolicy<OrderModel> securityPolicy()
		{
			return _ -> false;
		}

		@Override
		public de.gupta.clean.crud.template.useCases.incantation.domain.policy.profile.IncantationPolicyProfileResolver incantationPolicyProfileResolver()
		{
			return source -> switch (source)
			{
				case AUTHORITATIVE_EXTERNAL_EVENT -> IncantationPolicyProfile.authoritativeExternalEvent();
				default -> IncantationPolicyProfileResolver.defaultResolver().resolve(source);
			};
		}
	}

	private static final class QuarantiningAggregateDefinition extends TestAggregateDefinition
	{
		@Override
		public IncantationPolicyProfileResolver incantationPolicyProfileResolver()
		{
			return source -> source == IncantationSource.AUTHORITATIVE_EXTERNAL_EVENT
					? IncantationPolicyProfile.authoritativeExternalEvent()
					: IncantationPolicyProfileResolver.defaultResolver().resolve(source);
		}

		@Override
		public IncantationInvariantPolicy<OrderModel> incantationInvariantPolicy()
		{
			return (source, afterModel) -> source == IncantationSource.AUTHORITATIVE_EXTERNAL_EVENT
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
}