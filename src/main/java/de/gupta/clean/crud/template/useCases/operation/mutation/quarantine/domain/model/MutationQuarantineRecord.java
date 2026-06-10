package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model;

import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayEnvelope;
import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayOutcome;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.violation.MutationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.id.MutationQuarantineId;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.QuarantineLifecycleRecord;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MutationQuarantineRecord(
		MutationQuarantineId quarantineId,
		String aggregateType,
		QuarantineReplayEnvelope domainId,
		QuarantineReplayEnvelope payload,
		OperationSource source,
		OperationFamily family,
		Optional<OperationCorrelationId> correlationId,
		Optional<OperationCausationId> causationId,
		MutationQuarantineStatus status,
		List<MutationPolicyViolation> violations,
		Instant quarantinedAt,
		Instant updatedAt,
		int replayAttemptCount,
		Optional<Instant> lastReplayAt,
		Optional<QuarantineReplayOutcome> lastReplayOutcome,
		Optional<String> lastReplaySummary)
		implements QuarantineLifecycleRecord<MutationQuarantineId, MutationQuarantineRecord>
{
	public MutationQuarantineRecord
	{
		Objects.requireNonNull(quarantineId, "quarantineId");
		Objects.requireNonNull(aggregateType, "aggregateType");
		Objects.requireNonNull(domainId, "domainId");
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
		return status == MutationQuarantineStatus.OPEN;
	}

	@Override
	public String statusLabel()
	{
		return status.name();
	}

	@Override
	public MutationQuarantineRecord dismissed(final Instant at)
	{
		return new MutationQuarantineRecord(
				quarantineId,
				aggregateType,
				domainId,
				payload,
				source,
				family,
				correlationId,
				causationId,
				MutationQuarantineStatus.DISMISSED,
				violations,
				quarantinedAt,
				at,
				replayAttemptCount,
				lastReplayAt,
				lastReplayOutcome,
				lastReplaySummary);
	}

	@Override
	public MutationQuarantineRecord replayed(final Instant at)
	{
		return new MutationQuarantineRecord(
				quarantineId,
				aggregateType,
				domainId,
				payload,
				source,
				family,
				correlationId,
				causationId,
				MutationQuarantineStatus.REPLAYED,
				violations,
				quarantinedAt,
				at,
				replayAttemptCount + 1,
				Optional.of(at),
				Optional.of(QuarantineReplayOutcome.APPLIED),
				Optional.empty());
	}

	@Override
	public MutationQuarantineRecord replayAttempted(
			final Instant at,
			final QuarantineReplayOutcome replayOutcome,
			final Optional<String> replaySummary)
	{
		return new MutationQuarantineRecord(
				quarantineId,
				aggregateType,
				domainId,
				payload,
				source,
				family,
				correlationId,
				causationId,
				MutationQuarantineStatus.OPEN,
				violations,
				quarantinedAt,
				at,
				replayAttemptCount + 1,
				Optional.of(at),
				Optional.of(replayOutcome),
				replaySummary);
	}
}
