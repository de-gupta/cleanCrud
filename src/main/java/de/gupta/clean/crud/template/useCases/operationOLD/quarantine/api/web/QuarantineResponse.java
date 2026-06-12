package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.QuarantineReplayOutcome;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.QuarantineStatus;

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