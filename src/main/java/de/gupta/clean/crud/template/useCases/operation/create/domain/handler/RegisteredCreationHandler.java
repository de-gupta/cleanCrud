package de.gupta.clean.crud.template.useCases.operation.create.domain.handler;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;

import java.util.Objects;

public record RegisteredCreationHandler<Payload extends CreateOperationPayload, DomainCreateModel>(
		Class<Payload> payloadType, CreationHandler<Payload, DomainCreateModel> handler)
{
	public static <Payload extends CreateOperationPayload, DomainCreateModel> RegisteredCreationHandler<Payload, DomainCreateModel> of(
			final Class<Payload> payloadType, final CreationHandler<Payload, DomainCreateModel> handler)
	{
		return new RegisteredCreationHandler<>(payloadType, handler);
	}

	public RegisteredCreationHandler
	{
		Objects.requireNonNull(payloadType, "payloadType");
		Objects.requireNonNull(handler, "handler");
	}

	public boolean supportsExact(final Class<? extends CreateOperationPayload> candidateType)
	{
		return payloadType.equals(candidateType);
	}

	public boolean supportsAssignable(final Class<? extends CreateOperationPayload> candidateType)
	{
		return payloadType.isAssignableFrom(candidateType);
	}

	public CreationPlan<DomainCreateModel> createPlan(final CreationOperationRequest<?> request)
	{
		return handler.createPlan(request.withPayload(payloadType.cast(request.payload())));
	}
}