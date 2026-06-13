package de.gupta.clean.crud.template.useCases.operation.domain.metadata.request;

import java.util.Optional;

public record OperationRequestMetadata(OperationRequestSource source, Optional<OperationCorrelationId> correlationId,
                                       Optional<OperationCausationId> causationId)
{
}