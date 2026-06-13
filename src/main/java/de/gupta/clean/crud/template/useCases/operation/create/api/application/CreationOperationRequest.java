package de.gupta.clean.crud.template.useCases.operation.create.api.application;

import de.gupta.clean.crud.template.useCases.operation.domain.metadata.request.OperationRequestMetadata;

public record CreationOperationRequest<Payload extends CreateOperationPayload>(Payload payload,
                                                                               OperationRequestMetadata metadata)
{
}