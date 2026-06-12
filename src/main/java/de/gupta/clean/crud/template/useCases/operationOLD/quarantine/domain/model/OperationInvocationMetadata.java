package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.id.OperationCorrelationId;

import java.util.Objects;
import java.util.Optional;

public record OperationInvocationMetadata(
		OperationSource source,
		OperationFamily family,
		Optional<OperationCorrelationId> correlationId,
		Optional<OperationCausationId> causationId)
{
	public OperationInvocationMetadata
	{
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(family, "family");
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(causationId, "causationId");
	}
}