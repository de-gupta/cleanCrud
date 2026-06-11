package de.gupta.clean.crud.template.useCases.operation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.QuarantineReplayOutcome;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.QuarantineStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record QuarantineResponse(
		String quarantineId,
		String aggregateKey,
		OperationSource source,
		OperationFamily family,
		Optional<String> correlationId,
		Optional<String> causationId,
		QuarantineStatus status,
		List<OperationPolicyViolationResponse> violations,
		Instant quarantinedAt,
		Instant updatedAt,
		int replayAttemptCount,
		Optional<Instant> lastReplayAt,
		Optional<QuarantineReplayOutcome> lastReplayOutcome,
		Optional<String> lastReplaySummary)
{
}
