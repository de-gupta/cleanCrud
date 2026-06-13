package de.gupta.clean.crud.template.useCases.operation.create.application.internal;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationResult;

public interface CreateApplicationService<Payload extends CreateOperationPayload, DomainModel>
{
	CreationOperationResult<DomainModel> create(
			final CreationOperationRequest<Payload> request);
}