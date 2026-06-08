package de.gupta.clean.crud.template.useCases.process.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.clean.crud.template.useCases.process.application.dispatch.ApplicationActionDispatcher;
import de.gupta.clean.crud.template.useCases.process.application.execution.DefaultDurableProcessRunner;
import de.gupta.clean.crud.template.useCases.process.application.registration.DefaultDurableProcessDefinitionRegistry;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableRegisteredProcess;
import de.gupta.clean.crud.template.useCases.process.domain.action.ApplicationCommand;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessDefinition;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessTrigger;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.CorrelationId;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;
import de.gupta.clean.crud.template.useCases.process.domain.model.outcome.DurableProcessOutcome;
import de.gupta.clean.crud.template.useCases.process.domain.model.outcome.FailureClassification;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.BackoffPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.RetryPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTaskStatus;
import de.gupta.clean.crud.template.useCases.process.infrastructure.persistence.model.DurableProcessTaskPersistenceModel;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaDurableProcessTaskStoreTest.JpaEntityConfiguration.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class JpaDurableProcessTaskStoreTest
{
	@jakarta.annotation.Resource
	private EntityManager entityManager;

	private JpaDurableProcessTaskStore taskStore;

	@BeforeEach
	void setUp()
	{
		taskStore = JpaDurableProcessTaskStore.with(entityManager, new ObjectMapper());
	}

	@Test
	void saveUpdateAndFindByIdRoundTripRetryPolicyAndPayload()
	{
		var original = task("task-1", "submit-order", 0, Optional.empty(), 4, new BrokerPayload("payload-1"));

		taskStore.save(original);
		var updated = original.startAttempt(Instant.parse("2026-06-08T10:01:00Z"))
		                      .scheduleRetry(
									  Instant.parse("2026-06-08T10:02:00Z"),
									  Instant.parse("2026-06-08T10:03:00Z"),
									  Optional.of("temporary outage"),
									  Optional.of("RETRYING"));
		taskStore.update(updated);
		entityManager.flush();
		entityManager.clear();

		var reloaded = taskStore.findById(original.taskId()).orElseThrow();

		assertThat(reloaded.retryPolicy()).isEqualTo(original.retryPolicy());
		assertThat(reloaded.status()).isEqualTo(DurableProcessTaskStatus.WAITING_RETRY);
		assertThat(reloaded.nextAttemptAt()).contains(Instant.parse("2026-06-08T10:03:00Z"));
		assertThat(reloaded.lastFailureSummary()).contains("temporary outage");
		assertThat(reloaded.payload()).isEqualTo(original.payload());
	}

	@Test
	void findDueTasksOrdersByScheduledTimeThenCreationTime()
	{
		var retryPolicy = new RetryPolicy(3, BackoffPolicy.fixed(Duration.ofSeconds(5)));
		taskStore.save(task("later-created", "submit-order", 0, Optional.of(Instant.parse("2026-06-08T10:01:00Z")),
				retryPolicy, Instant.parse("2026-06-08T10:00:10Z"), new BrokerPayload("later-created")));
		taskStore.save(task("earlier-created", "submit-order", 0,
				Optional.of(Instant.parse("2026-06-08T10:01:00Z")), retryPolicy,
				Instant.parse("2026-06-08T10:00:00Z"), new BrokerPayload("earlier-created")));
		taskStore.save(task("not-due", "submit-order", 0, Optional.of(Instant.parse("2026-06-08T10:05:00Z")),
				retryPolicy, Instant.parse("2026-06-08T10:00:05Z"), new BrokerPayload("not-due")));

		entityManager.flush();
		entityManager.clear();

		assertThat(taskStore.findDueTasks(Instant.parse("2026-06-08T10:02:00Z"), 10))
				.extracting(DurableProcessTask::taskId)
				.containsExactly(new DurableProcessTaskId("earlier-created"),
						new DurableProcessTaskId("later-created"));
	}

	@Test
	void runnerPersistsSucceededRetryRejectedAndFailedOutcomes()
	{
		var dispatcher = new RecordingDispatcher();
		var fixedClock = Clock.fixed(Instant.parse("2026-06-08T10:05:00Z"), ZoneOffset.UTC);
		var successStore = JpaDurableProcessTaskStore.with(entityManager, new ObjectMapper());
		var successRunner = DefaultDurableProcessRunner.with(
				DefaultDurableProcessDefinitionRegistry.of(List.of(new DurableRegisteredProcess<>(
						DurableProcessDefinition.of("success-process", SubmitOrder.class, BrokerPayload.class),
						(payload, _) -> DurableProcessOutcome.succeeded(List.of(new PrintedCommand(payload.value())),
								"SUCCESS")))),
				successStore,
				successStore,
				dispatcher,
				fixedClock);
		var successTask = successStore.save(task("success", "success-process", 0, Optional.empty(), 2,
				new BrokerPayload("payload-success")));

		successRunner.runTask(successTask.taskId(), Instant.parse("2026-06-08T10:00:00Z"));
		entityManager.flush();
		entityManager.clear();
		assertThat(successStore.findById(successTask.taskId()).orElseThrow().status())
				.isEqualTo(DurableProcessTaskStatus.SUCCEEDED);

		var retryStore = JpaDurableProcessTaskStore.with(entityManager, new ObjectMapper());
		var retryRunner = DefaultDurableProcessRunner.with(
				DefaultDurableProcessDefinitionRegistry.of(List.of(new DurableRegisteredProcess<>(
						DurableProcessDefinition.of("retry-process", SubmitOrder.class, BrokerPayload.class),
						(_, _) -> DurableProcessOutcome.retryAt(
								Instant.parse("2026-06-08T10:10:00Z"),
								FailureClassification.TRANSIENT_TECHNICAL_FAILURE,
								"temporary outage")))),
				retryStore,
				retryStore,
				new RecordingDispatcher(),
				fixedClock);
		var retryTask = retryStore.save(task("retry", "retry-process", 0, Optional.empty(), 2,
				new BrokerPayload("payload-retry")));

		retryRunner.runTask(retryTask.taskId(), Instant.parse("2026-06-08T10:00:00Z"));
		entityManager.flush();
		entityManager.clear();
		assertThat(retryStore.findById(retryTask.taskId()).orElseThrow().status())
				.isEqualTo(DurableProcessTaskStatus.WAITING_RETRY);

		var exhaustedStore = JpaDurableProcessTaskStore.with(entityManager, new ObjectMapper());
		var exhaustedRunner = DefaultDurableProcessRunner.with(
				DefaultDurableProcessDefinitionRegistry.of(List.of(new DurableRegisteredProcess<>(
						DurableProcessDefinition.of("exhausted-process", SubmitOrder.class, BrokerPayload.class),
						(_, _) -> DurableProcessOutcome.retryAt(
								Instant.parse("2026-06-08T10:10:00Z"),
								FailureClassification.TRANSIENT_TECHNICAL_FAILURE,
								"temporary outage")))),
				exhaustedStore,
				exhaustedStore,
				new RecordingDispatcher(),
				fixedClock);
		var exhaustedTask = exhaustedStore.save(task("exhausted", "exhausted-process", 1, Optional.empty(), 1,
				new BrokerPayload("payload-exhausted")));

		exhaustedRunner.runTask(exhaustedTask.taskId(), Instant.parse("2026-06-08T10:00:00Z"));
		entityManager.flush();
		entityManager.clear();
		assertThat(exhaustedStore.findById(exhaustedTask.taskId()).orElseThrow().status())
				.isEqualTo(DurableProcessTaskStatus.FAILED);

		var rejectedStore = JpaDurableProcessTaskStore.with(entityManager, new ObjectMapper());
		var rejectedRunner = DefaultDurableProcessRunner.with(
				DefaultDurableProcessDefinitionRegistry.of(List.of(new DurableRegisteredProcess<>(
						DurableProcessDefinition.of("rejected-process", SubmitOrder.class, BrokerPayload.class),
						(_, _) -> DurableProcessOutcome.rejected("broker rejected order", List.of())))),
				rejectedStore,
				rejectedStore,
				new RecordingDispatcher(),
				fixedClock);
		var rejectedTask = rejectedStore.save(task("rejected", "rejected-process", 0, Optional.empty(), 2,
				new BrokerPayload("payload-rejected")));

		rejectedRunner.runTask(rejectedTask.taskId(), Instant.parse("2026-06-08T10:00:00Z"));
		entityManager.flush();
		entityManager.clear();
		assertThat(rejectedStore.findById(rejectedTask.taskId()).orElseThrow().status())
				.isEqualTo(DurableProcessTaskStatus.REJECTED);
	}

	private DurableProcessTask task(
			final String id,
			final String processType,
			final int attemptCount,
			final Optional<Instant> nextAttemptAt,
			final int maxAttempts,
			final BrokerPayload payload)
	{
		return task(id, processType, attemptCount, nextAttemptAt,
				new RetryPolicy(maxAttempts, BackoffPolicy.exponential(Duration.ofSeconds(2), 2.0d,
						Optional.of(Duration.ofSeconds(10)))),
				Instant.parse("2026-06-08T10:00:00Z"),
				payload);
	}

	private DurableProcessTask task(
			final String id,
			final String processType,
			final int attemptCount,
			final Optional<Instant> nextAttemptAt,
			final RetryPolicy retryPolicy,
			final Instant createdAt,
			final BrokerPayload payload)
	{
		return new DurableProcessTask(
				new DurableProcessTaskId(id),
				processType,
				new CorrelationId("corr-" + id),
				payload,
				retryPolicy,
				nextAttemptAt.isPresent() ? DurableProcessTaskStatus.WAITING_RETRY : DurableProcessTaskStatus.PENDING,
				attemptCount,
				nextAttemptAt,
				createdAt,
				createdAt,
				Optional.empty(),
				Optional.empty());
	}

	record SubmitOrder(String orderId) implements DurableProcessTrigger
	{
	}

	record BrokerPayload(String value) implements DurableProcessPayload
	{
	}

	record PrintedCommand(String value) implements ApplicationCommand
	{
	}

	private static final class RecordingDispatcher implements ApplicationActionDispatcher
	{
		private final List<de.gupta.clean.crud.template.useCases.process.domain.action.ApplicationAction> actions =
				new ArrayList<>();

		@Override
		public void dispatch(
				final Collection<? extends de.gupta.clean.crud.template.useCases.process.domain.action.ApplicationAction> applicationActions)
		{
			actions.addAll(applicationActions);
		}
	}

	@org.springframework.boot.SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = DurableProcessTaskPersistenceModel.class)
	static class JpaEntityConfiguration
	{
	}
}