package de.gupta.clean.crud.template.useCases.operation.create.domain.attempt;

import de.gupta.clean.crud.template.useCases.operation.create.domain.handler.RegisteredCreationHandler;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationContext;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;

import java.util.Objects;

public record PreparedCreationAttempt<Payload extends CreateOperationPayload, DomainCreateModel>(
		CreationOperationRequest<Payload> request,
		RegisteredCreationHandler<Payload, DomainCreateModel> handler,
		CreationOperationContext context,
		CreationPlan<DomainCreateModel> plan)
{
	public PreparedCreationAttempt
	{
		Objects.requireNonNull(request, "request");
		Objects.requireNonNull(handler, "handler");
		Objects.requireNonNull(context, "context");
		Objects.requireNonNull(plan, "plan");
	}

	public String aggregateKey()
	{
		return plan.aggregateKey();
	}
}
