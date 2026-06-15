package de.gupta.clean.crud.template.useCases.operation.create.application.service;

import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationRequestMetadata;
import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.EvaluatedCreationAttempt;
import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.PreparedCreationAttempt;
import de.gupta.clean.crud.template.useCases.operation.create.domain.execution.CreateExecutor;
import de.gupta.clean.crud.template.useCases.operation.create.domain.handler.CreationHandlerRegistry;
import de.gupta.clean.crud.template.useCases.operation.create.domain.handler.RegisteredCreationHandler;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;
import de.gupta.clean.crud.template.useCases.operation.create.domain.policy.CreationPolicyEvaluation;
import de.gupta.clean.crud.template.useCases.operation.create.domain.policy.CreationPolicyEvaluator;
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
		var executedAttempts = new ArrayList<PreparedCreationAttempt<TestPayload, String>>();
		var service = new TestCreateApplicationService(
				registryFor(request -> CreationPlan.of("aggregate.Task", request.payload().name())),
				_ -> CreationPolicyEvaluation.allow(List.of(tolerated)),
				attempt ->
				{
					executedAttempts.add(attempt);
					return "created:" + attempt.plan().domainModel();
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
		assertThat(executedAttempts).singleElement().satisfies(attempt ->
		{
			assertThat(attempt.request().payload()).isEqualTo(new TestPayload("draft"));
			assertThat(attempt.context().payloadTypeName()).isEqualTo(TestPayload.class.getName());
			assertThat(attempt.plan()).isEqualTo(CreationPlan.of("aggregate.Task", "draft"));
		});
	}

	private static CreationHandlerRegistry<String> registryFor(
			final de.gupta.clean.crud.template.useCases.operation.create.domain.handler.CreationHandler<TestPayload, String> handler)
	{
		return CreationHandlerRegistry.of(List.of(RegisteredCreationHandler.of(TestPayload.class, handler)));
	}

	private static CreateOperationRequest<TestPayload> request(final TestPayload payload)
	{
		return new CreateOperationRequest<>(
				payload,
				new OperationRequestMetadata(
						OperationSource.USER_INTENT,
						Optional.of(new OperationCorrelationId("corr-1")),
						Optional.of(new OperationCausationId("cause-1"))));
	}

	@Test
	void create_returnsRejectedResult_whenDecisionRejects()
	{
		var blocking = CreationOperationViolation.core("core rule failed");
		var tolerated = CreationOperationViolation.invariant("soft invariant warning");
		var executorCalls = new AtomicInteger();
		var service = new TestCreateApplicationService(
				registryFor(request -> CreationPlan.of("aggregate.Task", request.payload().name())),
				_ -> CreationPolicyEvaluation.reject(List.of(blocking), List.of(tolerated)),
				attempt ->
				{
					executorCalls.incrementAndGet();
					return "created:" + attempt.plan().domainModel();
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
				_ -> CreationPolicyEvaluation.quarantine(List.of(blocking), List.of(tolerated)),
				attempt -> "created:" + attempt.plan().domainModel(),
				recorder);

		var result = service.create(request(new TestPayload("draft")));

		assertThat(result).isInstanceOf(QuarantinedCreateOperationResult.class);
		var quarantined = (QuarantinedCreateOperationResult<String>) result;
		assertThat(quarantined.blockingViolations()).containsExactly(blocking);
		assertThat(quarantined.toleratedViolations()).containsExactly(tolerated);
		assertThat(quarantined.quarantineReference()).isEmpty();
		assertThat(recorder.recordedAttempts).singleElement().satisfies(recorded ->
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
				_ -> CreationPolicyEvaluation.quarantine(List.of(blocking)),
				attempt -> "created:" + attempt.plan().domainModel(),
				recorder);

		var result = service.create(request(new TestPayload("draft")));

		assertThat(result).isInstanceOf(QuarantinedCreateOperationResult.class);
		var quarantined = (QuarantinedCreateOperationResult<String>) result;
		assertThat(quarantined.quarantineReference()).contains("Q-42");
		assertThat(recorder.recordedAttempts).hasSize(1);
	}

	@Test
	void create_preservesPreexistingQuarantineReference_withoutCallingRecorder()
	{
		var blocking = CreationOperationViolation.access("manual review needed");
		var recorder = new TrackingQuarantineRecorder(Optional.of("Q-recorder"));
		var service = new TestCreateApplicationService(
				registryFor(request -> CreationPlan.of("aggregate.Task", request.payload().name())),
				_ -> CreationPolicyEvaluation.quarantine(List.of(blocking), List.of(), Optional.of("Q-existing")),
				attempt -> "created:" + attempt.plan().domainModel(),
				recorder);

		var result = service.create(request(new TestPayload("draft")));

		assertThat(result).isInstanceOf(QuarantinedCreateOperationResult.class);
		var quarantined = (QuarantinedCreateOperationResult<String>) result;
		assertThat(quarantined.quarantineReference()).contains("Q-existing");
		assertThat(recorder.recordedAttempts).isEmpty();
	}

	@Test
	void create_failsFast_whenNoHandlerIsRegistered()
	{
		var service = new TestCreateApplicationService(
				CreationHandlerRegistry.of(List.of()),
				CreationPolicyEvaluator.allowing(),
				attempt -> "created:" + attempt.plan().domainModel());

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

	private record TestPayload(String name) implements CreateOperationPayload
	{
	}

	private static final class TestCreateApplicationService
			extends AbstractCreateApplicationService<TestPayload, String>
	{
		private TestCreateApplicationService(
				final CreationHandlerRegistry<String> handlerRegistry,
				final CreationPolicyEvaluator policyEvaluator,
				final CreateExecutor<TestPayload, String> createExecutor)
		{
			super(handlerRegistry, policyEvaluator, createExecutor);
		}

		private TestCreateApplicationService(
				final CreationHandlerRegistry<String> handlerRegistry,
				final CreationPolicyEvaluator policyEvaluator,
				final CreateExecutor<TestPayload, String> createExecutor,
				final CreationQuarantineRecorder quarantineRecorder)
		{
			super(handlerRegistry, policyEvaluator, createExecutor, quarantineRecorder);
		}
	}

	private static final class TrackingQuarantineRecorder implements CreationQuarantineRecorder
	{
		private final Optional<String> persistedReference;
		private final List<EvaluatedCreationAttempt<?, ?>> recordedAttempts = new ArrayList<>();

		@Override
		public Optional<String> record(final EvaluatedCreationAttempt<?, ?> evaluatedAttempt)
		{
			recordedAttempts.add(evaluatedAttempt);
			return persistedReference;
		}

		private TrackingQuarantineRecorder(final Optional<String> persistedReference)
		{
			this.persistedReference = persistedReference;
		}
	}
}