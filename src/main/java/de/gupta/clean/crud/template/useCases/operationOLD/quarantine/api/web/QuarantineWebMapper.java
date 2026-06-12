package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.id.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.PayloadReplayInputs;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.QuarantineRecord;

public final class QuarantineWebMapper
{
	public QuarantineResponse toResponse(final QuarantineRecord<? extends PayloadReplayInputs> record)
	{
		return new QuarantineResponse(
				record.quarantineId().value(),
				record.aggregateKey(),
				record.metadata().source(),
				record.metadata().family(),
				record.metadata().correlationId().map(OperationCorrelationId::value),
				record.metadata().causationId().map(OperationCausationId::value),
				record.status(),
				record.violations().stream()
				      .map(v -> new OperationPolicyViolationResponse(v.kind(), v.message(), v.severity()))
				      .toList(),
				record.quarantinedAt(),
				record.updatedAt(),
				record.replayAttemptCount(),
				record.lastReplayAt(),
				record.lastReplayOutcome(),
				record.lastReplaySummary());
	}
}