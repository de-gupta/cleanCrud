package de.gupta.clean.crud.template.useCases.operation.create.domain.handler;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;

@FunctionalInterface
public interface CreationHandler<Payload extends CreateOperationPayload, DomainCreateModel>
{
	CreationPlan<DomainCreateModel> createPlan(CreateOperationRequest<Payload> request);
}