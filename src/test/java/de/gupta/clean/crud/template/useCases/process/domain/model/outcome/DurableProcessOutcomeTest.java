package de.gupta.clean.crud.template.useCases.process.domain.model.outcome;

import de.gupta.clean.crud.template.useCases.process.domain.action.ApplicationCommand;
import de.gupta.clean.crud.template.useCases.process.domain.action.ApplicationEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DurableProcessOutcomeTest
{
	@Test
	void successCarriesApplicationActionsAndIsTerminal()
	{
		var outcome = DurableProcessOutcome.succeeded(List.of(new TestCommand("submit"), new TestEvent("acked")),
				"OK");

		assertTrue(outcome.isTerminal());
		assertTrue(outcome.failureClassification().isEmpty());
		assertEquals("OK", outcome.outcomeCode().orElseThrow());
		assertEquals(2, outcome.applicationActions().size());
	}

	@Test
	void retryCarriesScheduleAndIsNotTerminal()
	{
		var retryAt = Instant.parse("2026-06-07T12:00:00Z");
		var outcome = new DurableProcessOutcome.Retry(
				retryAt,
				FailureClassification.TRANSIENT_TECHNICAL_FAILURE,
				java.util.Optional.of("temporary broker outage"),
				java.util.Optional.of("RETRY"),
				List.of());

		assertFalse(outcome.isTerminal());
		assertEquals(retryAt, outcome.nextAttemptAt());
		assertEquals(FailureClassification.TRANSIENT_TECHNICAL_FAILURE,
				outcome.failureClassification().orElseThrow());
	}

	@Test
	void rejectedCarriesBusinessFailureClassification()
	{
		var outcome = DurableProcessOutcome.rejected("broker rejected order", List.of(new TestEvent("rejected")));

		assertTrue(outcome.isTerminal());
		assertEquals(FailureClassification.BUSINESS_REJECTION, outcome.failureClassification().orElseThrow());
		assertEquals("broker rejected order", outcome.failureSummary().orElseThrow());
	}

	@Test
	void failedCarriesTechnicalFailureClassification()
	{
		var outcome = DurableProcessOutcome.failed(
				FailureClassification.PERMANENT_TECHNICAL_FAILURE,
				"exhausted retries",
				List.of(new TestCommand("fail-order")));

		assertTrue(outcome.isTerminal());
		assertEquals(FailureClassification.PERMANENT_TECHNICAL_FAILURE,
				outcome.failureClassification().orElseThrow());
		assertEquals("exhausted retries", outcome.failureSummary().orElseThrow());
	}

	private record TestCommand(String name) implements ApplicationCommand
	{
	}

	private record TestEvent(String name) implements ApplicationEvent
	{
	}
}