package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayOutcome;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record MutationQuarantineResponse(
		String quarantineId,
		String aggregateType,
		String payloadTypeName,
		OperationSource source,
		OperationFamily family,
		Optional<String> correlationId,
		Optional<String> causationId,
		MutationQuarantineStatus status,
		List<MutationPolicyViolationResponse> violations,
		Instant quarantinedAt,
		Instant updatedAt,
		int replayAttemptCount,
		Optional<Instant> lastReplayAt,
		Optional<QuarantineReplayOutcome> lastReplayOutcome,
		Optional<String> lastReplaySummary)
{
}
