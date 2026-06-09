package de.gupta.clean.crud.template.useCases.mutation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationFamily;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.MutationQuarantineStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record MutationQuarantineResponse(
		String quarantineId,
		String aggregateType,
		String domainIdType,
		String domainIdJson,
		String payloadType,
		String payloadJson,
		MutationSource source,
		MutationFamily family,
		Optional<String> correlationId,
		Optional<String> causationId,
		MutationQuarantineStatus status,
		List<MutationPolicyViolationResponse> violations,
		Instant quarantinedAt,
		Instant updatedAt,
		int replayAttemptCount,
		Optional<Instant> lastReplayAt,
		Optional<String> lastReplayOutcome,
		Optional<String> lastReplaySummary)
{
}
