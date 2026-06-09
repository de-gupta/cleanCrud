package de.gupta.clean.crud.template.useCases.mutation.domain.model;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.id.MutationCausationId;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.id.MutationCorrelationId;

import java.util.Objects;
import java.util.Optional;

public record MutationRequest<DomainId, MutationPayload extends ApplicationMutationPayload>(
		DomainId domainId,
		MutationPayload payload,
		MutationSource source,
		MutationFamily family,
		Optional<MutationCorrelationId> correlationId,
		Optional<MutationCausationId> causationId)
{
	public MutationRequest(
			final DomainId domainId,
			final MutationPayload payload,
			final MutationSource source)
	{
		this(domainId, payload, source, MutationFamily.APPLICATION, Optional.empty(), Optional.empty());
	}

	public MutationRequest(
			final DomainId domainId,
			final MutationPayload payload,
			final MutationSource source,
			final Optional<MutationCorrelationId> correlationId,
			final Optional<MutationCausationId> causationId)
	{
		this(domainId, payload, source, MutationFamily.APPLICATION, correlationId, causationId);
	}

	public MutationRequest
	{
		Objects.requireNonNull(domainId, "domainId");
		Objects.requireNonNull(payload, "payload");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(family, "family");
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(causationId, "causationId");
	}

	public Class<? extends ApplicationMutationPayload> payloadType()
	{
		return payload.getClass();
	}
}
