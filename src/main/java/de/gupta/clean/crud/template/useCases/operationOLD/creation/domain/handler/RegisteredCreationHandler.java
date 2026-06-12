package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.handler;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;

import java.util.Objects;

public record RegisteredCreationHandler<DomainModelCreate, CreationPayload extends ApplicationOperationPayload>(
		Class<CreationPayload> payloadType,
		AggregateCreationHandler<DomainModelCreate, CreationPayload> handler)
{
	public static <DomainModelCreate, CreationPayload extends ApplicationOperationPayload>
	RegisteredCreationHandler<DomainModelCreate, CreationPayload> of(
			final Class<CreationPayload> payloadType,
			final AggregateCreationHandler<DomainModelCreate, CreationPayload> handler)
	{
		return new RegisteredCreationHandler<>(payloadType, handler);
	}

	public RegisteredCreationHandler
	{
		Objects.requireNonNull(payloadType, "payloadType");
		Objects.requireNonNull(handler, "handler");
	}
}