package de.gupta.clean.crud.template.useCases.operation.common.domain.model;

import java.util.Optional;

public record OperationRequestMetadata(OperationSource source, Optional<OperationCorrelationId> correlationId,
                                       Optional<OperationCausationId> causationId)
{
	public static OperationRequestMetadata source(final OperationSource source)
	{
		return new OperationRequestMetadata(source, Optional.empty(), Optional.empty());
	}
}