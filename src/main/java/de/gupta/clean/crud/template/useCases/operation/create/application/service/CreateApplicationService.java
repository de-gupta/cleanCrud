package de.gupta.clean.crud.template.useCases.operation.create.application.service;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreateOperationResult;

public interface CreateApplicationService<Payload extends CreateOperationPayload, DomainModel>
{
	CreateOperationResult<DomainModel> create(final CreateOperationRequest<Payload> request);
}