package de.gupta.clean.crud.template.useCases.operation.create.domain.attempt;

import de.gupta.clean.crud.template.useCases.operation.create.domain.handler.RegisteredCreationHandler;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationContext;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;

import java.util.Objects;

public record PreparedCreationAttempt<DomainCreatePayload extends CreateOperationPayload, DomainModel>(
		CreateOperationRequest<DomainCreatePayload> request,
		RegisteredCreationHandler<DomainCreatePayload, DomainModel> handler,
		CreateOperationContext context,
		CreationPlan<DomainModel> plan)
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