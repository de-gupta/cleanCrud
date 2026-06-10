package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.violation.CreationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CreationQuarantineRecord(
		CreationQuarantineId quarantineId,
		String aggregateType,
		String payloadType,
		String payloadJson,
		OperationSource source,
		OperationFamily family,
		Optional<OperationCorrelationId> correlationId,
		Optional<OperationCausationId> causationId,
		CreationQuarantineStatus status,
		List<CreationPolicyViolation> violations,
		Instant quarantinedAt,
		Instant updatedAt,
		int replayAttemptCount,
		Optional<Instant> lastReplayAt,
		Optional<String> lastReplayOutcome,
		Optional<String> lastReplaySummary)
{
	public CreationQuarantineRecord
	{
		Objects.requireNonNull(quarantineId, "quarantineId");
		Objects.requireNonNull(aggregateType, "aggregateType");
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
		return status == CreationQuarantineStatus.OPEN;
	}

	public CreationQuarantineRecord dismissed(final Instant dismissedAt)
	{
		return new CreationQuarantineRecord(
				quarantineId,
				aggregateType,
				payloadType,
				payloadJson,
				source,
				family,
				correlationId,
				causationId,
				CreationQuarantineStatus.DISMISSED,
				violations,
				quarantinedAt,
				dismissedAt,
				replayAttemptCount,
				lastReplayAt,
				lastReplayOutcome,
				lastReplaySummary);
	}

	public CreationQuarantineRecord replayed(final Instant replayedAt)
	{
		return new CreationQuarantineRecord(
				quarantineId,
				aggregateType,
				payloadType,
				payloadJson,
				source,
				family,
				correlationId,
				causationId,
				CreationQuarantineStatus.REPLAYED,
				violations,
				quarantinedAt,
				replayedAt,
				replayAttemptCount + 1,
				Optional.of(replayedAt),
				Optional.of("APPLIED"),
				Optional.empty());
	}

	public CreationQuarantineRecord replayAttempted(
			final Instant replayedAt,
			final String replayOutcome,
			final Optional<String> replaySummary)
	{
		return new CreationQuarantineRecord(
				quarantineId,
				aggregateType,
				payloadType,
				payloadJson,
				source,
				family,
				correlationId,
				causationId,
				CreationQuarantineStatus.OPEN,
				violations,
				quarantinedAt,
				replayedAt,
				replayAttemptCount + 1,
				Optional.of(replayedAt),
				Optional.of(replayOutcome),
				replaySummary);
	}
}
