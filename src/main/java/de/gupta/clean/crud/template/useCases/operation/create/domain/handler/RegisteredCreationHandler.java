package de.gupta.clean.crud.template.useCases.operation.create.domain.handler;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;

import java.util.Objects;

public record RegisteredCreationHandler<DomainCreatePayload extends CreateOperationPayload, DomainModel>(
		Class<DomainCreatePayload> payloadType, CreationHandler<DomainCreatePayload, DomainModel> handler)
{
	public static <DomainCreatePayload extends CreateOperationPayload, DomainModel>
	RegisteredCreationHandler<DomainCreatePayload, DomainModel> of(final Class<DomainCreatePayload> payloadType,
	                                                               final CreationHandler<DomainCreatePayload, DomainModel> handler)
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

	public CreationPlan<DomainModel> createPlan(final CreateOperationRequest<?> request)
	{
		return handler.createPlan(request.withPayload(payloadType.cast(request.payload())));
	}
}