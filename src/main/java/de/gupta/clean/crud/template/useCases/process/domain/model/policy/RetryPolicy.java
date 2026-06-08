package de.gupta.clean.crud.template.useCases.process.domain.model.policy;

import java.time.Instant;
import java.util.Objects;

public record RetryPolicy(
		int maxAttempts,
		BackoffPolicy backoffPolicy)
{
	public RetryPolicy
	{
		if (maxAttempts < 1)
		{
			throw new IllegalArgumentException("maxAttempts");
		}
		Objects.requireNonNull(backoffPolicy, "backoffPolicy");
	}

	public boolean allowsRetry(final int attemptCount)
	{
		if (attemptCount < 0)
		{
			throw new IllegalArgumentException("attemptCount");
		}
		return attemptCount < maxAttempts;
	}

	public Instant nextAttemptAt(final Instant referenceTime, final int attemptNumber)
	{
		Objects.requireNonNull(referenceTime, "referenceTime");
		return referenceTime.plus(backoffPolicy.delayForAttempt(attemptNumber));
	}
}
