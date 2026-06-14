package de.gupta.clean.crud.template.useCases.operation.create.application.service;

import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationRequestMetadata;
import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.create.domain.execution.CreationExecutor;
import de.gupta.clean.crud.template.useCases.operation.create.domain.handler.CreationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operation.create.domain.handler.RegisteredCreationHandler;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;
import de.gupta.clean.crud.template.useCases.operation.create.domain.policy.CreationPolicyEvaluation;
import de.gupta.clean.crud.template.useCases.operation.create.domain.policy.CreationPolicyEvaluator;
import de.gupta.clean.crud.template.useCases.operation.create.domain.quarantine.CreationQuarantineRecordRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.quarantine.CreationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreatedCreateOperationResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationViolation;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.QuarantinedCreateOperationResult;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.RejectedCreateOperationResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractCreateApplicationServiceTest
{
	@Test
	void create_returnsCreatedResult_whenDecisionAllows()
	{
		var tolerated = CreationOperationViolation.externalConsistency("accepted with warning");
		var executedPlans = new ArrayList<CreationPlan<String>>();
		var service = new TestCreateApplicationService(
				registryFor(request -> CreationPlan.of("aggregate.Task", request.payload().name())),
				(_, _) -> CreationPolicyEvaluation.allow(List.of(tolerated)),
				plan ->
				{
					executedPlans.add(plan);
					return "created:" + plan.createModel();
				});

		var result = service.create(request(new TestPayload("draft")));

		assertThat(result).isInstanceOf(CreatedCreateOperationResult.class);
		var created = (CreatedCreateOperationResult<String>) result;
		assertThat(created.createdModel()).isEqualTo("created:draft");
		assertThat(created.toleratedViolations()).containsExactly(tolerated);
		assertThat(created.context().source()).isEqualTo(OperationSource.USER_INTENT);
		assertThat(created.context().payloadTypeName()).isEqualTo(TestPayload.class.getName());
		assertThat(created.context().correlationId()).contains("corr-1");
		assertThat(created.context().causationId()).contains("cause-1");
		assertThat(executedPlans).containsExactly(CreationPlan.of("aggregate.Task", "draft"));
	}

	@Test
	void create_returnsRejectedResult_whenDecisionRejects()
	{
		var blocking = CreationOperationViolation.core("core rule failed");
		var tolerated = CreationOperationViolation.invariant("soft invariant warning");
		var executorCalls = new AtomicInteger();
		var service = new TestCreateApplicationService(
				registryFor(request -> CreationPlan.of("aggregate.Task", request.payload().name())),
				(_, _) -> CreationPolicyEvaluation.reject(List.of(blocking), List.of(tolerated)),
				plan ->
				{
					executorCalls.incrementAndGet();
					return "created:" + plan.createModel();
				});

		var result = service.create(request(new TestPayload("draft")));

		assertThat(result).isInstanceOf(RejectedCreateOperationResult.class);
		var rejected = (RejectedCreateOperationResult<String>) result;
		assertThat(rejected.blockingViolations()).containsExactly(blocking);
		assertThat(rejected.toleratedViolations()).containsExactly(tolerated);
		assertThat(rejected.context().payloadTypeName()).isEqualTo(TestPayload.class.getName());
		assertThat(executorCalls).hasValue(0);
	}

	@Test
	void create_returnsQuarantinedResultWithoutReference_whenRecorderDoesNotPersist()
	{
		var blocking = CreationOperationViolation.access("manual review needed");
		var tolerated = CreationOperationViolation.invariant("soft warning");
		var recorder = new TrackingQuarantineRecorder(Optional.empty());
		var service = new TestCreateApplicationService(
				registryFor(request -> CreationPlan.of("aggregate.Task", request.payload().name())),
				(_, _) -> CreationPolicyEvaluation.quarantine(List.of(blocking), List.of(tolerated)),
				plan -> "created:" + plan.createModel(),
				recorder);

		var result = service.create(request(new TestPayload("draft")));

		assertThat(result).isInstanceOf(QuarantinedCreateOperationResult.class);
		var quarantined = (QuarantinedCreateOperationResult<String>) result;
		assertThat(quarantined.blockingViolations()).containsExactly(blocking);
		assertThat(quarantined.toleratedViolations()).containsExactly(tolerated);
		assertThat(quarantined.quarantineReference()).isEmpty();
		assertThat(recorder.recordedRequests).singleElement().satisfies(recorded ->
		{
			assertThat(recorded.aggregateKey()).isEqualTo("aggregate.Task");
			assertThat(recorded.context().payloadTypeName()).isEqualTo(TestPayload.class.getName());
			assertThat(recorded.blockingViolations()).containsExactly(blocking);
		});
	}

	@Test
	void create_returnsQuarantinedResultWithReference_whenRecorderPersists()
	{
		var blocking = CreationOperationViolation.access("manual review needed");
		var recorder = new TrackingQuarantineRecorder(Optional.of("Q-42"));
		var service = new TestCreateApplicationService(
				registryFor(request -> CreationPlan.of("aggregate.Task", request.payload().name())),
				(_, _) -> CreationPolicyEvaluation.quarantine(List.of(blocking)),
				plan -> "created:" + plan.createModel(),
				recorder);

		var result = service.create(request(new TestPayload("draft")));

		assertThat(result).isInstanceOf(QuarantinedCreateOperationResult.class);
		var quarantined = (QuarantinedCreateOperationResult<String>) result;
		assertThat(quarantined.quarantineReference()).contains("Q-42");
		assertThat(recorder.recordedRequests).hasSize(1);
	}

	@Test
	void create_failsFast_whenNoHandlerIsRegistered()
	{
		var service = new TestCreateApplicationService(
				CreationHandlerRegistry.of(List.of()),
				CreationPolicyEvaluator.allowing(),
				plan -> "created:" + plan.createModel());

		assertThatThrownBy(() -> service.create(request(new TestPayload("draft"))))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(TestPayload.class.getName());
	}

	@Test
	void registry_failsFast_whenDuplicateHandlerRegistrationExists()
	{
		assertThatThrownBy(() -> CreationHandlerRegistry.of(List.of(
				RegisteredCreationHandler.of(TestPayload.class, _ -> CreationPlan.of("aggregate.Task", "a")),
				RegisteredCreationHandler.of(TestPayload.class, _ -> CreationPlan.of("aggregate.Task", "b")))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(TestPayload.class.getName());
	}

	private static CreationHandlerRegistry<String> registryFor(
			final de.gupta.clean.crud.template.useCases.operation.create.domain.handler.CreationHandler<TestPayload, String> handler)
	{
		return CreationHandlerRegistry.of(List.of(RegisteredCreationHandler.of(TestPayload.class, handler)));
	}

	private static CreationOperationRequest<TestPayload> request(final TestPayload payload)
	{
		return new CreationOperationRequest<>(
				payload,
				new OperationRequestMetadata(
						OperationSource.USER_INTENT,
						Optional.of(new OperationCorrelationId("corr-1")),
						Optional.of(new OperationCausationId("cause-1"))));
	}

	private record TestPayload(String name) implements CreateOperationPayload
	{
	}

	private static final class TestCreateApplicationService
			extends AbstractCreateApplicationService<TestPayload, String, String>
	{
		private TestCreateApplicationService(
				final CreationHandlerRegistry<String> handlerRegistry,
				final CreationPolicyEvaluator policyEvaluator,
				final CreationExecutor<String, String> creationExecutor)
		{
			super(handlerRegistry, policyEvaluator, creationExecutor);
		}

		private TestCreateApplicationService(
				final CreationHandlerRegistry<String> handlerRegistry,
				final CreationPolicyEvaluator policyEvaluator,
				final CreationExecutor<String, String> creationExecutor,
				final CreationQuarantineRecorder quarantineRecorder)
		{
			super(handlerRegistry, policyEvaluator, creationExecutor, quarantineRecorder);
		}
	}

	private static final class TrackingQuarantineRecorder implements CreationQuarantineRecorder
	{
		private final Optional<String> persistedReference;
		private final List<CreationQuarantineRecordRequest> recordedRequests = new ArrayList<>();

		@Override
		public Optional<String> record(final CreationQuarantineRecordRequest request)
		{
			recordedRequests.add(request);
			return persistedReference;
		}

		private TrackingQuarantineRecorder(final Optional<String> persistedReference)
		{
			this.persistedReference = persistedReference;
		}
	}
}