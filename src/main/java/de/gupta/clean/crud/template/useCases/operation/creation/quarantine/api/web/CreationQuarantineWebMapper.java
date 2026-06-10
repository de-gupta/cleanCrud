package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.model.CreationQuarantineRecord;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.invariant.InvariantViolation;

public final class CreationQuarantineWebMapper
{
	public CreationQuarantineResponse toResponse(final CreationQuarantineRecord record)
	{
		return new CreationQuarantineResponse(
				record.quarantineId().value(),
				record.aggregateType(),
				record.payloadType(),
				record.payloadJson(),
				record.source(),
				record.family(),
				record.correlationId().map(OperationCorrelationId::value),
				record.causationId().map(OperationCausationId::value),
				record.status(),
				record.violations().stream()
				      .map(violation -> new CreationPolicyViolationResponse(
							  violation.kind(),
							  violation.message(),
							  violation.invariantViolation().map(InvariantViolation::severity)))
				      .toList(),
				record.quarantinedAt(),
				record.updatedAt(),
				record.replayAttemptCount(),
				record.lastReplayAt(),
				record.lastReplayOutcome(),
				record.lastReplaySummary());
	}
}