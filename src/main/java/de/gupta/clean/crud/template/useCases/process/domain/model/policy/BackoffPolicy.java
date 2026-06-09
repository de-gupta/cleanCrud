package de.gupta.clean.crud.template.useCases.process.domain.model.policy;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public record BackoffPolicy(
		Duration initialDelay,
		double multiplier,
		Optional<Duration> maxDelay)
{
	public static BackoffPolicy fixed(final Duration delay)
	{
		return new BackoffPolicy(delay, 1.0d, Optional.of(delay));
	}

	public static BackoffPolicy exponential(
			final Duration initialDelay,
			final double multiplier,
			final Optional<Duration> maxDelay)
	{
		return new BackoffPolicy(initialDelay, multiplier, maxDelay);
	}

	public BackoffPolicy
	{
		Objects.requireNonNull(initialDelay, "initialDelay");
		if (initialDelay.isNegative())
		{
			throw new IllegalArgumentException("initialDelay");
		}
		if (multiplier <= 0.0d)
		{
			throw new IllegalArgumentException("multiplier");
		}
		Objects.requireNonNull(maxDelay, "maxDelay");
		maxDelay.ifPresent(delay ->
		{
			if (delay.isNegative())
			{
				throw new IllegalArgumentException("maxDelay");
			}
		});
	}

	public Duration delayForAttempt(final int attemptNumber)
	{
		if (attemptNumber < 1)
		{
			throw new IllegalArgumentException("attemptNumber");
		}
		double unboundedMillis = initialDelay.toMillis() * Math.pow(multiplier, attemptNumber - 1);
		Duration calculated = Duration.ofMillis(Math.max(0L, Math.round(unboundedMillis)));
		return maxDelay.filter(limit -> calculated.compareTo(limit) > 0).orElse(calculated);
	}
}