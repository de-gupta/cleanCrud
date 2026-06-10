package de.gupta.clean.crud.template.useCases.incantation.domain.handler;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.ApplicationIncantationPayload;
import de.gupta.clean.crud.template.useCases.incantation.domain.plan.AggregateIncantationPlan;

import java.util.Objects;

public record RegisteredIncantationHandler<DomainModelCreate, IncantationPayload extends ApplicationIncantationPayload>(
		Class<IncantationPayload> payloadType,
		AggregateIncantationHandler<DomainModelCreate, IncantationPayload> handler)
{
	public static <DomainModelCreate, IncantationPayload extends ApplicationIncantationPayload>
	RegisteredIncantationHandler<DomainModelCreate, IncantationPayload> ofAggregate(
			final Class<IncantationPayload> payloadType,
			final AggregateIncantationHandler<DomainModelCreate, IncantationPayload> handler)
	{
		return new RegisteredIncantationHandler<>(payloadType, handler);
	}

	public static <DomainModelCreate, IncantationPayload extends ApplicationIncantationPayload>
	RegisteredIncantationHandler<DomainModelCreate, IncantationPayload> of(
			final Class<IncantationPayload> payloadType,
			final IncantationHandler<DomainModelCreate, IncantationPayload> handler)
	{
		return ofAggregate(payloadType, payload -> AggregateIncantationPlan.rootOnly(handler.apply(payload)));
	}

	public RegisteredIncantationHandler
	{
		Objects.requireNonNull(payloadType, "payloadType");
		Objects.requireNonNull(handler, "handler");
	}
}
