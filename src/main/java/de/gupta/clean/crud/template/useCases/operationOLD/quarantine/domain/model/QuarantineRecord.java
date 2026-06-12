package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.QuarantineReplayOutcome;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.violation.OperationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.QuarantineLifecycleRecord;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record QuarantineRecord<P extends PayloadReplayInputs>(
		QuarantineId quarantineId,
		String aggregateKey,
		OperationInvocationMetadata metadata,
		P replayInputs,
		List<OperationPolicyViolation> violations,
		QuarantineStatus status,
		Instant quarantinedAt,
		Instant updatedAt,
		int replayAttemptCount,
		Optional<Instant> lastReplayAt,
		Optional<QuarantineReplayOutcome> lastReplayOutcome,
		Optional<String> lastReplaySummary)
		implements QuarantineLifecycleRecord<QuarantineId, QuarantineRecord<P>>
{
	public QuarantineRecord
	{
		Objects.requireNonNull(quarantineId, "quarantineId");
		Objects.requireNonNull(aggregateKey, "aggregateKey");
		Objects.requireNonNull(metadata, "metadata");
		Objects.requireNonNull(replayInputs, "replayInputs");
		violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
		Objects.requireNonNull(status, "status");
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

	@Override
	public String aggregateType()
	{
		return aggregateKey;
	}

	@Override
	public boolean open()
	{
		return status == QuarantineStatus.OPEN;
	}

	@Override
	public String statusLabel()
	{
		return status.name();
	}

	@Override
	public QuarantineRecord<P> dismissed(final Instant at)
	{
		return new QuarantineRecord<>(
				quarantineId,
				aggregateKey,
				metadata,
				replayInputs,
				violations,
				QuarantineStatus.DISMISSED,
				quarantinedAt,
				at,
				replayAttemptCount,
				lastReplayAt,
				lastReplayOutcome,
				lastReplaySummary);
	}

	@Override
	public QuarantineRecord<P> replayed(final Instant at)
	{
		return new QuarantineRecord<>(
				quarantineId,
				aggregateKey,
				metadata,
				replayInputs,
				violations,
				QuarantineStatus.REPLAYED,
				quarantinedAt,
				at,
				replayAttemptCount + 1,
				Optional.of(at),
				Optional.of(QuarantineReplayOutcome.APPLIED),
				Optional.empty());
	}

	@Override
	public QuarantineRecord<P> replayAttempted(
			final Instant at,
			final QuarantineReplayOutcome outcome,
			final Optional<String> summary)
	{
		return new QuarantineRecord<>(
				quarantineId,
				aggregateKey,
				metadata,
				replayInputs,
				violations,
				QuarantineStatus.OPEN,
				quarantinedAt,
				at,
				replayAttemptCount + 1,
				Optional.of(at),
				Optional.of(outcome),
				summary);
	}

	public String payloadTypeName()
	{
		return replayInputs.payload().typeKey();
	}
}