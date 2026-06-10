package de.gupta.clean.crud.template.useCases.operation.creation.domain.model;

import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;

import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;

import java.util.Objects;
import java.util.Optional;

public record CreationRequest<Payload extends ApplicationOperationPayload>(
		Payload payload,
		OperationSource source,
		OperationFamily family,
		Optional<OperationCorrelationId> correlationId,
		Optional<OperationCausationId> causationId)
{
	public CreationRequest(
			final Payload payload,
			final OperationSource source)
	{
		this(payload, source, OperationFamily.APPLICATION, Optional.empty(), Optional.empty());
	}

	public CreationRequest(
			final Payload payload,
			final OperationSource source,
			final Optional<OperationCorrelationId> correlationId,
			final Optional<OperationCausationId> causationId)
	{
		this(payload, source, OperationFamily.APPLICATION, correlationId, causationId);
	}

	public CreationRequest
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
