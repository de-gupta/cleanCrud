package de.gupta.clean.crud.template.useCases.incantation.domain.model;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.id.IncantationCausationId;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.id.IncantationCorrelationId;

import java.util.Objects;
import java.util.Optional;

public record IncantationRequest<Payload extends ApplicationIncantationPayload>(
		Payload payload,
		IncantationSource source,
		IncantationFamily family,
		Optional<IncantationCorrelationId> correlationId,
		Optional<IncantationCausationId> causationId)
{
	public IncantationRequest(
			final Payload payload,
			final IncantationSource source)
	{
		this(payload, source, IncantationFamily.APPLICATION, Optional.empty(), Optional.empty());
	}

	public IncantationRequest(
			final Payload payload,
			final IncantationSource source,
			final Optional<IncantationCorrelationId> correlationId,
			final Optional<IncantationCausationId> causationId)
	{
		this(payload, source, IncantationFamily.APPLICATION, correlationId, causationId);
	}

	public IncantationRequest
	{
		Objects.requireNonNull(payload, "payload");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(family, "family");
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(causationId, "causationId");
	}

	public Class<?> payloadType()
	{
		return payload.getClass();
	}
}
