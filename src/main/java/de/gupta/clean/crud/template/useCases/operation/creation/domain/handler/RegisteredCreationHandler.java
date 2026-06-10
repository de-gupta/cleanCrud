package de.gupta.clean.crud.template.useCases.operation.creation.domain.handler;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.plan.AggregateCreationPlan;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;

import java.util.Objects;

public record RegisteredCreationHandler<DomainModelCreate, CreationPayload extends ApplicationOperationPayload>(
		Class<CreationPayload> payloadType,
		AggregateCreationHandler<DomainModelCreate, CreationPayload> handler)
{
	public static <DomainModelCreate, CreationPayload extends ApplicationOperationPayload>
	RegisteredCreationHandler<DomainModelCreate, CreationPayload> ofAggregate(
			final Class<CreationPayload> payloadType,
			final AggregateCreationHandler<DomainModelCreate, CreationPayload> handler)
	{
		return new RegisteredCreationHandler<>(payloadType, handler);
	}

	public static <DomainModelCreate, CreationPayload extends ApplicationOperationPayload>
	RegisteredCreationHandler<DomainModelCreate, CreationPayload> of(
			final Class<CreationPayload> payloadType,
			final CreationHandler<DomainModelCreate, CreationPayload> handler)
	{
		return ofAggregate(payloadType, payload -> AggregateCreationPlan.rootOnly(handler.apply(payload)));
	}

	public RegisteredCreationHandler
	{
		Objects.requireNonNull(payloadType, "payloadType");
		Objects.requireNonNull(handler, "handler");
	}
}