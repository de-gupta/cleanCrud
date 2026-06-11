package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.model.MutationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.quarantine.api.web.OperationPolicyViolationResponse;

public final class MutationQuarantineWebMapper
{
	public MutationQuarantineResponse toResponse(final MutationQuarantineRecord record)
	{
		return new MutationQuarantineResponse(
				record.quarantineId().value(),
				record.aggregateType(),
				record.payload().typeKey(),
				record.source(),
				record.family(),
				record.correlationId().map(OperationCorrelationId::value),
				record.causationId().map(OperationCausationId::value),
				record.status(),
				record.violations().stream()
				      .map(violation -> new OperationPolicyViolationResponse(
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
