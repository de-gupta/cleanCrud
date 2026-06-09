package de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationFamily;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.id.MutationCausationId;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.id.MutationCorrelationId;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation.MutationPolicyViolation;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.id.MutationQuarantineId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MutationQuarantineRecord(
		MutationQuarantineId quarantineId,
		String aggregateType,
		String domainIdType,
		String domainIdJson,
		String payloadType,
		String payloadJson,
		MutationSource source,
		MutationFamily family,
		Optional<MutationCorrelationId> correlationId,
		Optional<MutationCausationId> causationId,
		MutationQuarantineStatus status,
		List<MutationPolicyViolation> violations,
		Instant quarantinedAt,
		Instant updatedAt,
		int replayAttemptCount,
		Optional<Instant> lastReplayAt,
		Optional<String> lastReplayOutcome,
		Optional<String> lastReplaySummary)
{
	public MutationQuarantineRecord
	{
		Objects.requireNonNull(quarantineId, "quarantineId");
		Objects.requireNonNull(aggregateType, "aggregateType");
		Objects.requireNonNull(domainIdType, "domainIdType");
		Objects.requireNonNull(domainIdJson, "domainIdJson");
		Objects.requireNonNull(payloadType, "payloadType");
		Objects.requireNonNull(payloadJson, "payloadJson");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(family, "family");
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(causationId, "causationId");
		Objects.requireNonNull(status, "status");
		violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
		Objects.requireNonNull(quarantinedAt, "quarantinedAt");
		Objects.requireNonNull(updatedAt, "updatedAt");
		Objects.requireNonNull(lastReplayAt, "lastReplayAt");
		Objects.requireNonNull(lastReplayOutcome, "lastReplayOutcome");
		Objects.requireNonNull(lastReplaySummary, "lastReplaySummary");
		if (replayAttemptCount < 0)
		{
			throw new IllegalArgumentException("replayAttemptCount");
		}
	}

	public boolean open()
	{
		return status == MutationQuarantineStatus.OPEN;
	}

	public MutationQuarantineRecord dismissed(final Instant dismissedAt)
	{
		return new MutationQuarantineRecord(
				quarantineId,
				aggregateType,
				domainIdType,
				domainIdJson,
				payloadType,
				payloadJson,
				source,
				family,
				correlationId,
				causationId,
				MutationQuarantineStatus.DISMISSED,
				violations,
				quarantinedAt,
				dismissedAt,
				replayAttemptCount,
				lastReplayAt,
				lastReplayOutcome,
				lastReplaySummary);
	}

	public MutationQuarantineRecord replayed(final Instant replayedAt)
	{
		return new MutationQuarantineRecord(
				quarantineId,
				aggregateType,
				domainIdType,
				domainIdJson,
				payloadType,
				payloadJson,
				source,
				family,
				correlationId,
				causationId,
				MutationQuarantineStatus.REPLAYED,
				violations,
				quarantinedAt,
				replayedAt,
				replayAttemptCount + 1,
				Optional.of(replayedAt),
				Optional.of("APPLIED"),
				Optional.empty());
	}

	public MutationQuarantineRecord replayAttempted(
			final Instant replayedAt,
			final String replayOutcome,
			final Optional<String> replaySummary)
	{
		return new MutationQuarantineRecord(
				quarantineId,
				aggregateType,
				domainIdType,
				domainIdJson,
				payloadType,
				payloadJson,
				source,
				family,
				correlationId,
				causationId,
				MutationQuarantineStatus.OPEN,
				violations,
				quarantinedAt,
				replayedAt,
				replayAttemptCount + 1,
				Optional.of(replayedAt),
				Optional.of(replayOutcome),
				replaySummary);
	}
}
