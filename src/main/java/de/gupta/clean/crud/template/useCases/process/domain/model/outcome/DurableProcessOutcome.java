package de.gupta.clean.crud.template.useCases.process.domain.model.outcome;

import de.gupta.clean.crud.template.useCases.process.domain.action.ApplicationAction;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public sealed interface DurableProcessOutcome
		permits DurableProcessOutcome.Success, DurableProcessOutcome.Retry,
		DurableProcessOutcome.Rejected, DurableProcessOutcome.Failed
{
	static DurableProcessOutcome succeeded(final Collection<? extends ApplicationAction> applicationActions)
	{
		return new Success(cast(applicationActions), Optional.empty());
	}

	static DurableProcessOutcome succeeded(
			final Collection<? extends ApplicationAction> applicationActions,
			final String outcomeCode)
	{
		return new Success(cast(applicationActions), Optional.of(outcomeCode));
	}

	static DurableProcessOutcome retryAt(
			final Instant nextAttemptAt,
			final FailureClassification failureClassification,
			final String failureSummary)
	{
		return new Retry(nextAttemptAt, failureClassification, Optional.ofNullable(failureSummary), Optional.empty(),
				List.of());
	}

	static DurableProcessOutcome rejected(
			final String failureSummary,
			final Collection<? extends ApplicationAction> applicationActions)
	{
		return new Rejected(Optional.ofNullable(failureSummary), Optional.empty(), cast(applicationActions));
	}

	static DurableProcessOutcome failed(
			final FailureClassification failureClassification,
			final String failureSummary,
			final Collection<? extends ApplicationAction> applicationActions)
	{
		return new Failed(failureClassification, Optional.ofNullable(failureSummary), Optional.empty(),
				cast(applicationActions));
	}

	Collection<ApplicationAction> applicationActions();

	Optional<String> outcomeCode();

	default Optional<FailureClassification> failureClassification()
	{
		return Optional.empty();
	}

	default Optional<String> failureSummary()
	{
		return Optional.empty();
	}

	default boolean isTerminal()
	{
		return switch (this)
		{
			case Success _, Rejected _, Failed _ -> true;
			case Retry _ -> false;
		};
	}

	private static List<ApplicationAction> cast(final Collection<? extends ApplicationAction> applicationActions)
	{
		return List.copyOf(Objects.requireNonNull(applicationActions, "applicationActions"));
	}

	record Success(
			Collection<ApplicationAction> applicationActions,
			Optional<String> outcomeCode)
			implements DurableProcessOutcome
	{
		public Success
		{
			applicationActions = List.copyOf(Objects.requireNonNull(applicationActions, "applicationActions"));
			outcomeCode = Objects.requireNonNull(outcomeCode, "outcomeCode");
		}
	}

	record Retry(
			Instant nextAttemptAt,
			FailureClassification classification,
			Optional<String> failureSummary,
			Optional<String> outcomeCode,
			Collection<ApplicationAction> applicationActions)
			implements DurableProcessOutcome
	{
		public Retry
		{
			nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt");
			classification = Objects.requireNonNull(classification, "classification");
			if (!classification.isRetryable())
			{
				throw new IllegalArgumentException("classification");
			}
			failureSummary = Objects.requireNonNull(failureSummary, "failureSummary");
			outcomeCode = Objects.requireNonNull(outcomeCode, "outcomeCode");
			applicationActions = List.copyOf(Objects.requireNonNull(applicationActions, "applicationActions"));
		}

		@Override
		public Optional<FailureClassification> failureClassification()
		{
			return Optional.of(classification);
		}
	}

	record Rejected(
			Optional<String> failureSummary,
			Optional<String> outcomeCode,
			Collection<ApplicationAction> applicationActions)
			implements DurableProcessOutcome
	{
		public Rejected
		{
			failureSummary = Objects.requireNonNull(failureSummary, "failureSummary");
			outcomeCode = Objects.requireNonNull(outcomeCode, "outcomeCode");
			applicationActions = List.copyOf(Objects.requireNonNull(applicationActions, "applicationActions"));
		}

		@Override
		public Optional<FailureClassification> failureClassification()
		{
			return Optional.of(FailureClassification.BUSINESS_REJECTION);
		}
	}

	record Failed(
			FailureClassification classification,
			Optional<String> failureSummary,
			Optional<String> outcomeCode,
			Collection<ApplicationAction> applicationActions)
			implements DurableProcessOutcome
	{
		public Failed
		{
			classification = Objects.requireNonNull(classification, "classification");
			if (!classification.isTechnical())
			{
				throw new IllegalArgumentException("classification");
			}
			failureSummary = Objects.requireNonNull(failureSummary, "failureSummary");
			outcomeCode = Objects.requireNonNull(outcomeCode, "outcomeCode");
			applicationActions = List.copyOf(Objects.requireNonNull(applicationActions, "applicationActions"));
		}

		@Override
		public Optional<FailureClassification> failureClassification()
		{
			return Optional.of(classification);
		}
	}
}