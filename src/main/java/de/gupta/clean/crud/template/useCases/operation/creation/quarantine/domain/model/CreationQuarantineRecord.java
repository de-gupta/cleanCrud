package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.violation.CreationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.id.CreationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayEnvelope;
import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayOutcome;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.QuarantineLifecycleRecord;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CreationQuarantineRecord(
		CreationQuarantineId quarantineId,
		String aggregateType,
		QuarantineReplayEnvelope payload,
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
		Optional<QuarantineReplayOutcome> lastReplayOutcome,
		Optional<String> lastReplaySummary)
		implements QuarantineLifecycleRecord<CreationQuarantineId, CreationQuarantineRecord>
{
	public CreationQuarantineRecord
	{
		Objects.requireNonNull(quarantineId, "quarantineId");
		Objects.requireNonNull(aggregateType, "aggregateType");
		Objects.requireNonNull(payload, "payload");
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

	@Override
	public boolean open()
	{
		return status == CreationQuarantineStatus.OPEN;
	}

	@Override
	public String statusLabel()
	{
		return status.name();
	}

	@Override
	public CreationQuarantineRecord dismissed(final Instant at)
	{
		return new CreationQuarantineRecord(
				quarantineId,
				aggregateType,
				payload,
				source,
				family,
				correlationId,
				causationId,
				CreationQuarantineStatus.DISMISSED,
				violations,
				quarantinedAt,
				at,
				replayAttemptCount,
				lastReplayAt,
				lastReplayOutcome,
				lastReplaySummary);
	}

	@Override
	public CreationQuarantineRecord replayed(final Instant at)
	{
		return new CreationQuarantineRecord(
				quarantineId,
				aggregateType,
				payload,
				source,
				family,
				correlationId,
				causationId,
				CreationQuarantineStatus.REPLAYED,
				violations,
				quarantinedAt,
				at,
				replayAttemptCount + 1,
				Optional.of(at),
				Optional.of(QuarantineReplayOutcome.APPLIED),
				Optional.empty());
	}

	@Override
	public CreationQuarantineRecord replayAttempted(
			final Instant at,
			final QuarantineReplayOutcome replayOutcome,
			final Optional<String> replaySummary)
	{
		return new CreationQuarantineRecord(
				quarantineId,
				aggregateType,
				payload,
				source,
				family,
				correlationId,
				causationId,
				CreationQuarantineStatus.OPEN,
				violations,
				quarantinedAt,
				at,
				replayAttemptCount + 1,
				Optional.of(at),
				Optional.of(replayOutcome),
				replaySummary);
	}
}
