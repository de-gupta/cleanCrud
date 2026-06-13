package de.gupta.clean.crud.template.useCases.operation.create.application.service;

import de.gupta.clean.crud.template.useCases.operation.domain.metadata.request.OperationRequestMetadata;
import de.gupta.clean.crud.template.useCases.operation.domain.metadata.result.CreationOperationResult;

public interface CreateApplicationService<DomainID, DomainModelCreate, DomainModelResponse>
{
	CreationOperationResult<DomainID, DomainModelResponse> create(final DomainModelCreate model,
	                                                              final OperationRequestMetadata requestMetadata);
}