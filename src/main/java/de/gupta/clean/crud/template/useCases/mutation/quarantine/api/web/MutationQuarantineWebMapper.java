package de.gupta.clean.crud.template.useCases.mutation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.id.MutationCausationId;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.id.MutationCorrelationId;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.model.MutationQuarantineRecord;

public final class MutationQuarantineWebMapper
{
	public MutationQuarantineResponse toResponse(final MutationQuarantineRecord record)
	{
		return new MutationQuarantineResponse(
				record.quarantineId().value(),
				record.aggregateType(),
				record.domainIdType(),
				record.domainIdJson(),
				record.payloadType(),
				record.payloadJson(),
				record.source(),
				record.family(),
				record.correlationId().map(MutationCorrelationId::value),
				record.causationId().map(MutationCausationId::value),
				record.status(),
				record.violations().stream()
				      .map(violation -> new MutationPolicyViolationResponse(
							  violation.kind(),
							  violation.message(),
							  violation.severity()))
				      .toList(),
				record.quarantinedAt(),
				record.updatedAt(),
				record.replayAttemptCount(),
				record.lastReplayAt(),
				record.lastReplayOutcome(),
				record.lastReplaySummary());
	}
}