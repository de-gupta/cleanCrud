package de.gupta.clean.crud.template.useCases.mutation.domain.handler;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.ApplicationMutationPayload;

import java.util.Objects;

public record RegisteredMutationHandler<DomainModel, MutationPayload extends ApplicationMutationPayload>(
		Class<MutationPayload> payloadType,
		MutationHandler<DomainModel, MutationPayload> handler)
{
	public static <DomainModel, MutationPayload extends ApplicationMutationPayload>
	RegisteredMutationHandler<DomainModel, MutationPayload> of(
			final Class<MutationPayload> payloadType,
			final MutationHandler<DomainModel, MutationPayload> handler)
	{
		return new RegisteredMutationHandler<>(payloadType, handler);
	}

	public RegisteredMutationHandler
	{
		Objects.requireNonNull(payloadType, "payloadType");
		Objects.requireNonNull(handler, "handler");
	}

	public boolean supports(final Class<? extends ApplicationMutationPayload> candidateType)
	{
		return payloadType.equals(candidateType);
	}
}
