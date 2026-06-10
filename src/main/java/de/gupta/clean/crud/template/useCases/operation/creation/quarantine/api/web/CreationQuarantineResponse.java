package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineStatus;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record CreationQuarantineResponse(
		String quarantineId,
		String aggregateType,
		String payloadType,
		String payloadJson,
		OperationSource source,
		OperationFamily family,
		Optional<String> correlationId,
		Optional<String> causationId,
		CreationQuarantineStatus status,
		List<CreationPolicyViolationResponse> violations,
		Instant quarantinedAt,
		Instant updatedAt,
		int replayAttemptCount,
		Optional<Instant> lastReplayAt,
		Optional<String> lastReplayOutcome,
		Optional<String> lastReplaySummary)
{
}
