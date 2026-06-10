package de.gupta.clean.crud.template.useCases.operation.mutation.domain.handler;

import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.plan.AggregateMutationPlan;

import java.util.Objects;

public record RegisteredMutationHandler<DomainModel, MutationPayload extends ApplicationOperationPayload>(
		Class<MutationPayload> payloadType,
		AggregateMutationHandler<DomainModel, MutationPayload> handler)
{
	public static <DomainModel, MutationPayload extends ApplicationOperationPayload>
	RegisteredMutationHandler<DomainModel, MutationPayload> of(
			final Class<MutationPayload> payloadType,
			final MutationHandler<DomainModel, MutationPayload> handler)
	{
		return new RegisteredMutationHandler<>(payloadType,
				(currentModel, payload) -> AggregateMutationPlan.rootOnly(handler.apply(currentModel, payload)));
	}

	public static <DomainModel, MutationPayload extends ApplicationOperationPayload>
	RegisteredMutationHandler<DomainModel, MutationPayload> ofAggregate(
			final Class<MutationPayload> payloadType,
			final AggregateMutationHandler<DomainModel, MutationPayload> handler)
	{
		return new RegisteredMutationHandler<>(payloadType, handler);
	}

	public RegisteredMutationHandler
	{
		Objects.requireNonNull(payloadType, "payloadType");
		Objects.requireNonNull(handler, "handler");
	}

	public boolean supports(final Class<? extends ApplicationOperationPayload> candidateType)
	{
		return payloadType.equals(candidateType);
	}
}
