package de.gupta.clean.crud.template.useCases.operationOLD.mutation.aggregate.service;

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
import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceNotFoundException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.relationship.LifecycleSemantics;
import de.gupta.clean.crud.template.domain.relationship.ReconciliationStrategy;
import de.gupta.clean.crud.template.domain.relationship.RelationshipKind;
import de.gupta.clean.crud.template.domain.service.crud.policy.DeletionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.InsertionPolicy;
import de.gupta.clean.crud.template.domain.service.crud.policy.PatchPolicy;
import de.gupta.clean.crud.template.domain.service.equality.DuplicateDefinition;
import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.id.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.invariant.InvariantViolation;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.violation.ViolationHandling;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.violation.ViolationKind;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.application.service.MutationService;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.handler.MutationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.handler.RegisteredMutationHandler;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.model.MutationContext;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.model.MutationResult;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.plan.AggregateMutationPlan;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.invariant.DomainInvariantPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.profile.MutationPolicyProfile;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.profile.MutationPolicyProfileResolver;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.quarantine.application.recording.MutationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.quarantine.application.recording.MutationQuarantineSubmission;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service.DefaultQuarantineReplayRegistry;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service.QuarantineReplayCommand;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service.QuarantineReplayGateway;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.MutationReplayData;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.OperationInvocationMetadata;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.QuarantineId;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AggregateMutationServicesTest
{
	@Test
	void mutationServiceAppliesRegisteredHandlerAndPersistsUpdatedModel()
	{
		var definition = new TestAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = mutationService("test-aggregate", definition, engine);

		var updated = service.mutate(new MutationRequest<>(
				"order-1",
				new AcknowledgeOrder(),
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT)).updatedOrThrow();

		assertEquals("order-1", updated.id());
		assertEquals("ACKNOWLEDGED", updated.model().status());
		assertEquals("ACKNOWLEDGED", definition.store.get("order-1").status());
	}

	private MutationService<String, OrderModel> mutationService(
			final String aggregateKey,
			final TestAggregateDefinition definition,
			final AggregateLifecycle engine)
	{
		return AggregateMutationServices.mutationService(
				aggregateKey,
				definition,
				engine,
				registry(),
				MutationQuarantineRecorder.noop());
	}

	private MutationHandlerRegistry<OrderModel> registry()
	{
		return MutationHandlerRegistry.of(List.of(
				RegisteredMutationHandler.of(AcknowledgeOrder.class,
						(_, _) -> AggregateMutationPlan.rootOnly(new OrderModel("ACKNOWLEDGED")))));
	}

	@Test
	void mutateWithResultPersistsQuarantineIdWhenRecorderConfigured()
	{
		var definition = new QuarantiningAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		var recorder = new RecordingMutationQuarantineRecorder();
		var engine = DefaultAggregateLifecycle.withTransactionRunnerAndMutationQuarantineRecorder(
				new InlineTransactionRunner());
		var service = mutationService("test-aggregate", definition, engine);

		var result = service.mutateWithResult(new MutationRequest<>(
				"order-1",
				new AcknowledgeOrder(),
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT));

		assertTrue(result.quarantined());
		assertTrue(result.quarantineRequest().orElseThrow().quarantineId().isPresent());
		assertEquals(1, recorder.submissions.size());
	}

	@Test
	void quarantineReplayRegistryAcceptsMultipleAggregateMutationServices()
	{
		var firstDefinition = new TestAggregateDefinition();
		var secondDefinition = new AlternateAggregateDefinition();
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var firstService = mutationService("test-aggregate", firstDefinition, engine);
		var secondService =
				AggregateMutationServices.mutationService(
						"test-aggregate-2",
						secondDefinition,
						engine,
						registry(),
						MutationQuarantineRecorder.noop());

		@SuppressWarnings("unchecked")
		var gateways = List.of(
				(de.gupta.clean.crud.template.useCases.operationOLD.quarantine.application.service.QuarantineReplayGateway<de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.MutationReplayData>) firstService,
				secondService);
		assertDoesNotThrow(() -> DefaultQuarantineReplayRegistry.of(gateways));
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
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = mutationService("test-aggregate", definition, engine);

		service.mutate(new MutationRequest<>("order-1", new AcknowledgeOrder(), OperationSource.INTERNAL_COMMAND));

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
		var engine = DefaultAggregateLifecycle.withTransactionRunnerAndDurableProcessStarter(
				new InlineTransactionRunner(),
				new RecordingDurableProcessStarter(startedRequests));
		var processDefinition = DurableProcessDefinition.of("order-follow-up", OrderMutated.class, OrderPayload.class);
		var retryPolicy = new RetryPolicy(3, BackoffPolicy.fixed(Duration.ofSeconds(1)));
		var service = AggregateMutationServices.mutationService(
				"test-aggregate",
				definition,
				engine,
				registry(),
				context -> List.of(new DurableProcessStartRequest<>(
						processDefinition,
						new OrderMutated(context.domainId()),
						new OrderPayload(context.afterModel().orElseThrow().status()),
						new CorrelationId("mutation:" + context.domainId()),
						retryPolicy)),
				MutationQuarantineRecorder.noop());

		service.mutate(new MutationRequest<>("order-1", new AcknowledgeOrder(), OperationSource.INTERNAL_COMMAND));

		assertEquals(1, startedRequests.size());
		assertEquals("order-follow-up", startedRequests.getFirst().definition().processType());
	}

	@Test
	void mutationServicePassesCorrelationAndCausationMetadataToDurableProcessMapping()
	{
		var definition = new TestAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		var observedContexts = new ArrayList<MutationContext<String, OrderModel>>();
		var engine = DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = AggregateMutationServices.mutationService(
				"test-aggregate",
				definition,
				engine,
				registry(),
				context ->
				{
					observedContexts.add(context);
					return List.of();
				},
				MutationQuarantineRecorder.noop());

		service.mutate(new MutationRequest<>(
				"order-1",
				new AcknowledgeOrder(),
				OperationSource.PROCESS_EMITTED_ACTION,
				Optional.of(new OperationCorrelationId("corr-1")),
				Optional.of(new OperationCausationId("cause-1"))));

		assertEquals(1, observedContexts.size());
		assertEquals(OperationFamily.APPLICATION, observedContexts.getFirst().family());
		assertEquals(AcknowledgeOrder.class, observedContexts.getFirst().payloadType());
		assertEquals("corr-1", observedContexts.getFirst().correlationId().orElseThrow().value());
		assertEquals("cause-1", observedContexts.getFirst().causationId().orElseThrow().value());
	}

	@Test
	void mutationServiceRejectsUnknownPayloadType()
	{
		var definition = new TestAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = AggregateMutationServices.mutationService("test-aggregate", definition, engine,
				MutationHandlerRegistry.of(List.of()),
				MutationQuarantineRecorder.noop());

		var exception = assertThrows(
				InvalidRequestException.class,
				() -> service.mutate(new MutationRequest<>(
						"order-1",
						new RejectOrder("risk"),
						OperationSource.AUTHORITATIVE_EXTERNAL_EVENT)));

		assertTrue(exception.getMessage().contains(RejectOrder.class.getName()));
	}

	@Test
	void userIntentMutationsStillRespectAccessPolicy()
	{
		var definition = new AccessDeniedAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = mutationService("test-aggregate", definition, engine);

		assertThrows(
				AccessDeniedException.class,
				() -> service.mutate(new MutationRequest<>(
						"order-1",
						new AcknowledgeOrder(),
						OperationSource.USER_INTENT)));
	}

	@Test
	void authoritativeExternalEventsBypassAccessPolicyButStillMutate()
	{
		var definition = new AccessDeniedAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = mutationService("test-aggregate", definition, engine);

		var updated = service.mutate(new MutationRequest<>(
				"order-1",
				new AcknowledgeOrder(),
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT)).updatedOrThrow();

		assertEquals("ACKNOWLEDGED", updated.model().status());
	}

	@Test
	void authoritativeExternalEventsStillRespectInvariantPolicy()
	{
		var definition = new InvariantRejectingAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = mutationService("test-aggregate", definition, engine);

		assertThrows(
				InvalidRequestException.class,
				() -> service.mutate(new MutationRequest<>(
						"order-1",
						new AcknowledgeOrder(),
						OperationSource.AUTHORITATIVE_EXTERNAL_EVENT)));
	}

	@Test
	void authoritativeExternalEventsCanBeQuarantinedByHardInvariantPolicy()
	{
		var definition = new QuarantiningAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = mutationService("test-aggregate", definition, engine);

		var result = service.mutate(new MutationRequest<>(
				"order-1",
				new AcknowledgeOrder(),
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT));

		assertTrue(result.quarantined());
		assertEquals(1, result.quarantineRequest().orElseThrow().violations().size());
		assertEquals(ViolationKind.INVARIANT,
				result.quarantineRequest().orElseThrow().violations().getFirst().kind());
	}

	@Test
	void mutateWithResultCanExposeQuarantineWithoutThrowing()
	{
		var definition = new QuarantiningAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = mutationService("test-aggregate", definition, engine);

		var result = service.mutateWithResult(new MutationRequest<>(
				"order-1",
				new AcknowledgeOrder(),
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT));

		assertTrue(result.quarantined());
		assertTrue(result.updated().isEmpty());
		assertEquals(1, result.quarantineRequest().orElseThrow().violations().size());
		assertEquals("SUBMITTED", definition.store.get("order-1").status());
	}

	@Test
	void replayGatewayDoesNotCreateNestedQuarantineRecordsWhenReplayQuarantinesAgain()
	{
		var definition = new ReplayQuarantiningAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		var recorder = new RecordingMutationQuarantineRecorder();
		var engine = DefaultAggregateLifecycle.withTransactionRunnerAndMutationQuarantineRecorder(
				new InlineTransactionRunner());
		var service = mutationService("test-aggregate", definition, engine);
		@SuppressWarnings("unchecked")
		var replayGateway = (QuarantineReplayGateway<MutationReplayData>) service;

		var result = replayGateway.replay(new QuarantineReplayCommand<>(
				new QuarantineId("quarantine-1"),
				new MutationReplayData("order-1", new AcknowledgeOrder()),
				new OperationInvocationMetadata(
						OperationSource.ADMINISTRATIVE_REPLAY,
						OperationFamily.APPLICATION,
						Optional.empty(),
						Optional.empty())));

		assertTrue(result.quarantined());
		assertEquals(0, recorder.submissions.size());
	}

	@Test
	void registryRejectsDuplicatePayloadTypeRegistrations()
	{
		assertThrows(
				IllegalArgumentException.class,
				() -> MutationHandlerRegistry.of(List.of(
						RegisteredMutationHandler.of(AcknowledgeOrder.class, (OrderModel _,
						                                                      AcknowledgeOrder ignored) -> AggregateMutationPlan.rootOnly(
								new OrderModel("ACKNOWLEDGED"))),
						RegisteredMutationHandler.of(AcknowledgeOrder.class, (OrderModel currentModel,
						                                                      AcknowledgeOrder ignored) -> AggregateMutationPlan.rootOnly(
								currentModel)))));
	}

	@Test
	void quarantineReplayGatewayReturnsExplicitAggregateKey()
	{
		var definition = new TestAggregateDefinition();
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		@SuppressWarnings("unchecked")
		var gateway =
				(QuarantineReplayGateway<MutationReplayData>) mutationService("order-mutation", definition,
						engine);

		assertThat(gateway.aggregateKey())
				.as("aggregateKey should return the explicit key passed at construction")
				.isEqualTo("order-mutation");
	}

	@Test
	void softInvariantViolationsCanBeAllowedForAuthoritativeEventsAndBeExposedOnResult()
	{
		var definition = new SoftInvariantAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = mutationService("test-aggregate", definition, engine);

		MutationResult<String, OrderModel> result = service.mutateWithResult(new MutationRequest<>(
				"order-1",
				new AcknowledgeOrder(),
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT));

		assertTrue(result.applied());
		assertEquals("ACKNOWLEDGED", result.updatedOrThrow().model().status());
		assertEquals(1, result.toleratedViolations().size());
		assertEquals(ViolationKind.INVARIANT, result.toleratedViolations().getFirst().kind());
	}

	@Test
	void mutationServiceRejectsMissingAggregate()
	{
		var definition = new TestAggregateDefinition();
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = mutationService("test-aggregate", definition, engine);

		assertThrows(
				ResourceNotFoundException.class,
				() -> service.mutate(new MutationRequest<>(
						"missing",
						new AcknowledgeOrder(),
						OperationSource.AUTHORITATIVE_EXTERNAL_EVENT)));
	}

	@Test
	void rootOnlyMutationStillWorksForRelationshipAggregates()
	{
		var definition = new RelationshipAggregateDefinition();
		definition.store.put("order-1", new OrderModel("SUBMITTED"));
		AggregateLifecycle engine =
				DefaultAggregateLifecycle.withTransactionRunner(new InlineTransactionRunner());
		var service = mutationService("test-aggregate", definition, engine);

		var updated = service.mutate(new MutationRequest<>(
				"order-1",
				new AcknowledgeOrder(),
				OperationSource.INTERNAL_COMMAND)).updatedOrThrow();

		assertEquals("ACKNOWLEDGED", updated.model().status());
	}

	private record OrderModel(String status)
	{
	}

	private record AcknowledgeOrder() implements ApplicationOperationPayload
	{
	}

	private record RejectOrder(String reason) implements ApplicationOperationPayload
	{
	}

	private record OrderMutated(String id) implements DurableProcessTrigger
	{
	}

	private record OrderPayload(String status) implements DurableProcessPayload
	{
	}

	private static class TestAggregateDefinition
			implements AggregateDefinition<String, OrderModel, String, String, String>
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
		public DeletionPolicy<OrderModel> deletionPolicy()
		{
			return _ ->
			{
			};
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

		@Override
		public DomainSecurityPolicy<OrderModel> securityPolicy()
		{
			return DomainSecurityPolicy.allowing();
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

	private static final class RelationshipAggregateDefinition extends TestAggregateDefinition
	{
		@Override
		public Collection<AggregateRelationshipDefinitionContract<String, OrderModel, String, String>>
		relationshipDefinitions()
		{
			return List.of(new TestRelationshipDefinition());
		}
	}

	private static final class AccessDeniedAggregateDefinition extends TestAggregateDefinition
	{
		@Override
		public DomainSecurityPolicy<OrderModel> securityPolicy()
		{
			return _ -> false;
		}
	}

	private static final class AlternateAggregateDefinition extends TestAggregateDefinition
	{
		@Override
		public AggregateFetchPort<String, OrderModel> fetchPort()
		{
			return new AlternateFetchPort(store);
		}
	}

	private static final class InvariantRejectingAggregateDefinition extends TestAggregateDefinition
	{
		@Override
		public PatchPolicy<OrderModel> patchPolicy()
		{
			return (_, _) ->
			{
				throw InvalidRequestException.withMessage("Invariant rejected mutation");
			};
		}
	}

	private static class QuarantiningAggregateDefinition extends TestAggregateDefinition
	{
		@Override
		public DomainInvariantPolicy<OrderModel> domainInvariantPolicy()
		{
			return (_, _, _) -> List.of(InvariantViolation.hard("External state must be quarantined"));
		}
	}

	private static final class SoftInvariantAggregateDefinition extends TestAggregateDefinition
	{
		@Override
		public MutationPolicyProfileResolver mutationPolicyProfileResolver()
		{
			return source -> switch (source)
			{
				case AUTHORITATIVE_EXTERNAL_EVENT -> new MutationPolicyProfile(
						ViolationHandling.ALLOW,
						ViolationHandling.REJECT,
						ViolationHandling.QUARANTINE,
						ViolationHandling.ALLOW,
						ViolationHandling.QUARANTINE);
				case USER_INTENT -> MutationPolicyProfile.userIntent();
				case INTERNAL_COMMAND, PROCESS_EMITTED_ACTION, ADMINISTRATIVE_REPLAY ->
						MutationPolicyProfile.internalCommand();
			};
		}

		@Override
		public DomainInvariantPolicy<OrderModel> domainInvariantPolicy()
		{
			return (_, _, _) -> List.of(InvariantViolation.soft("External soft invariant violation"));
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
		public RelationshipKind relationshipKind()
		{
			return RelationshipKind.OWNED;
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
		public AggregateDefinition<Long, Long, Long, Long, ?> satelliteDefinition()
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
			implements AggregateDefinition<Long, Long, Long, Long, Long>
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
		public DeletionPolicy<Long> deletionPolicy()
		{
			return _ ->
			{
			};
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

		@Override
		public DomainSecurityPolicy<Long> securityPolicy()
		{
			return DomainSecurityPolicy.allowing();
		}

		@Override
		public PatchPolicy<Long> patchPolicy()
		{
			return (_, _) ->
			{
			};
		}

		@Override
		public InsertionPolicy<Long> insertionPolicy()
		{
			return _ ->
			{
			};
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

	private record AlternateFetchPort(Map<String, OrderModel> store) implements AggregateFetchPort<String, OrderModel>
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

	private static final class ReplayQuarantiningAggregateDefinition extends QuarantiningAggregateDefinition
	{
		@Override
		public MutationPolicyProfileResolver mutationPolicyProfileResolver()
		{
			return source -> switch (source)
			{
				case AUTHORITATIVE_EXTERNAL_EVENT, ADMINISTRATIVE_REPLAY ->
						MutationPolicyProfile.authoritativeExternalEvent();
				case USER_INTENT -> MutationPolicyProfile.userIntent();
				case INTERNAL_COMMAND, PROCESS_EMITTED_ACTION -> MutationPolicyProfile.internalCommand();
			};
		}
	}

	private static final class RecordingMutationQuarantineRecorder implements MutationQuarantineRecorder
	{
		private final List<MutationQuarantineSubmission> submissions = new ArrayList<>();

		@Override
		public de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.quarantine.MutationQuarantineRequest record(
				final MutationQuarantineSubmission submission)
		{
			submissions.add(submission);
			return submission.quarantineRequest()
			                 .persistedAs(
									 new de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.QuarantineId(
											 "stored-" + submissions.size()));
		}
	}
}