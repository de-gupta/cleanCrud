package de.gupta.clean.crud.template.useCases.process.domain.model.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurableProcessTaskStatusTest
{
	@Test
	void terminalStatusesAreMarkedTerminal()
	{
		assertTrue(DurableProcessTaskStatus.SUCCEEDED.isTerminal());
		assertTrue(DurableProcessTaskStatus.REJECTED.isTerminal());
		assertTrue(DurableProcessTaskStatus.FAILED.isTerminal());
		assertTrue(DurableProcessTaskStatus.CANCELLED.isTerminal());
		assertFalse(DurableProcessTaskStatus.PENDING.isTerminal());
		assertFalse(DurableProcessTaskStatus.RUNNING.isTerminal());
		assertFalse(DurableProcessTaskStatus.WAITING_RETRY.isTerminal());
	}

	@Test
	void onlyPendingAndWaitingRetryAreReadyForExecution()
	{
		assertTrue(DurableProcessTaskStatus.PENDING.isReadyForExecution());
		assertTrue(DurableProcessTaskStatus.WAITING_RETRY.isReadyForExecution());
		assertFalse(DurableProcessTaskStatus.RUNNING.isReadyForExecution());
		assertFalse(DurableProcessTaskStatus.SUCCEEDED.isReadyForExecution());
		assertFalse(DurableProcessTaskStatus.REJECTED.isReadyForExecution());
		assertFalse(DurableProcessTaskStatus.FAILED.isReadyForExecution());
		assertFalse(DurableProcessTaskStatus.CANCELLED.isReadyForExecution());
	}
}
