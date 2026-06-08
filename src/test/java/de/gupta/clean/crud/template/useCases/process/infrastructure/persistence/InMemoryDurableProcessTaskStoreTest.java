package de.gupta.clean.crud.template.useCases.process.infrastructure.persistence;

import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.CorrelationId;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.BackoffPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.RetryPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTask;
import de.gupta.clean.crud.template.useCases.process.domain.model.task.DurableProcessTaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryDurableProcessTaskStoreTest
{
	@Test
	void findDueTasksReturnsOnlyDueTasksInExecutionOrder()
	{
		var store = InMemoryDurableProcessTaskStore.create();
		var now = Instant.parse("2026-06-08T10:00:00Z");
		var retryPolicy = new RetryPolicy(3, BackoffPolicy.fixed(Duration.ofSeconds(5)));
		var dueLaterCreated = task("later-created", now.minusSeconds(5), Optional.of(now.minusSeconds(1)), retryPolicy);
		var dueEarlierCreated = task("earlier-created", now.minusSeconds(10), Optional.of(now.minusSeconds(1)),
				retryPolicy);
		var notDue = task("not-due", now.minusSeconds(20), Optional.of(now.plusSeconds(30)), retryPolicy);

		store.save(dueLaterCreated);
		store.save(notDue);
		store.save(dueEarlierCreated);

		assertEquals(
				java.util.List.of(dueEarlierCreated.taskId(), dueLaterCreated.taskId()),
				store.findDueTasks(now, 10).stream().map(DurableProcessTask::taskId).toList());
	}

	private DurableProcessTask task(
			final String id,
			final Instant createdAt,
			final Optional<Instant> nextAttemptAt,
			final RetryPolicy retryPolicy)
	{
		return new DurableProcessTask(
				new DurableProcessTaskId(id),
				"test-process",
				new CorrelationId("corr:" + id),
				new TestPayload(id),
				retryPolicy,
				nextAttemptAt.isPresent() ? DurableProcessTaskStatus.WAITING_RETRY : DurableProcessTaskStatus.PENDING,
				0,
				nextAttemptAt,
				createdAt,
				createdAt,
				Optional.empty(),
				Optional.empty());
	}

	private record TestPayload(String value) implements DurableProcessPayload
	{
	}
}
