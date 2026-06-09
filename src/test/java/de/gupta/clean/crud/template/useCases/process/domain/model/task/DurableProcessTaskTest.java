package de.gupta.clean.crud.template.useCases.process.domain.model.task;

import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.CorrelationId;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.BackoffPolicy;
import de.gupta.clean.crud.template.useCases.process.domain.model.policy.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DurableProcessTaskTest
{
	@Test
	void tasksAreValueObjectsAndPendingTasksWithoutScheduleAreImmediatelyDue()
	{
		var createdAt = Instant.parse("2026-06-07T12:00:00Z");
		var first = new DurableProcessTask(
				new DurableProcessTaskId("task-1"),
				"submit-order",
				new CorrelationId("corr-1"),
				new TestPayload("payload"),
				new RetryPolicy(3, BackoffPolicy.fixed(Duration.ofSeconds(1))),
				DurableProcessTaskStatus.PENDING,
				0,
				Optional.empty(),
				createdAt,
				createdAt,
				Optional.empty(),
				Optional.empty());
		var second = new DurableProcessTask(
				new DurableProcessTaskId("task-1"),
				"submit-order",
				new CorrelationId("corr-1"),
				new TestPayload("payload"),
				new RetryPolicy(3, BackoffPolicy.fixed(Duration.ofSeconds(1))),
				DurableProcessTaskStatus.PENDING,
				0,
				Optional.empty(),
				createdAt,
				createdAt,
				Optional.empty(),
				Optional.empty());

		assertEquals(first, second);
		assertTrue(first.isDueAt(createdAt));
		assertFalse(first.isTerminal());
	}

	@Test
	void waitingRetryTasksHonorScheduledAttemptTime()
	{
		var nextAttemptAt = Instant.parse("2026-06-07T12:05:00Z");
		var task = new DurableProcessTask(
				new DurableProcessTaskId("task-2"),
				"submit-order",
				new CorrelationId("corr-2"),
				new TestPayload("payload"),
				new RetryPolicy(3, BackoffPolicy.fixed(Duration.ofSeconds(1))),
				DurableProcessTaskStatus.WAITING_RETRY,
				1,
				Optional.of(nextAttemptAt),
				Instant.parse("2026-06-07T12:00:00Z"),
				Instant.parse("2026-06-07T12:01:00Z"),
				Optional.of("temporary failure"),
				Optional.of("RETRY"));

		assertFalse(task.isDueAt(nextAttemptAt.minusSeconds(1)));
		assertTrue(task.isDueAt(nextAttemptAt));
	}

	private record TestPayload(String value) implements DurableProcessPayload
	{
	}
}