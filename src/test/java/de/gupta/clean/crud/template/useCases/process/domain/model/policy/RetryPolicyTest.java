package de.gupta.clean.crud.template.useCases.process.domain.model.policy;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest
{
	@Test
	void fixedBackoffReturnsSameDelayForEveryAttempt()
	{
		var backoffPolicy = BackoffPolicy.fixed(Duration.ofSeconds(5));

		assertEquals(Duration.ofSeconds(5), backoffPolicy.delayForAttempt(1));
		assertEquals(Duration.ofSeconds(5), backoffPolicy.delayForAttempt(3));
	}

	@Test
	void exponentialBackoffGrowsAndRespectsMaximumDelay()
	{
		var backoffPolicy = BackoffPolicy.exponential(Duration.ofSeconds(2), 2.0d, Optional.of(Duration.ofSeconds(5)));

		assertEquals(Duration.ofSeconds(2), backoffPolicy.delayForAttempt(1));
		assertEquals(Duration.ofSeconds(4), backoffPolicy.delayForAttempt(2));
		assertEquals(Duration.ofSeconds(5), backoffPolicy.delayForAttempt(3));
	}

	@Test
	void retryPolicyLimitsAttemptsAndSchedulesNextAttempt()
	{
		var retryPolicy = new RetryPolicy(3, BackoffPolicy.fixed(Duration.ofSeconds(10)));
		var referenceTime = Instant.parse("2026-06-07T12:00:00Z");

		assertTrue(retryPolicy.allowsRetry(0));
		assertTrue(retryPolicy.allowsRetry(2));
		assertFalse(retryPolicy.allowsRetry(3));
		assertEquals(Instant.parse("2026-06-07T12:00:10Z"), retryPolicy.nextAttemptAt(referenceTime, 1));
	}
}