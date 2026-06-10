package de.gupta.clean.crud.template.useCases.operation.mutation.domain.model;

import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;

import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;

import java.util.Objects;
import java.util.Optional;

public record MutationRequest<DomainId, MutationPayload extends ApplicationOperationPayload>(
		DomainId domainId,
		MutationPayload payload,
		OperationSource source,
		OperationFamily family,
		Optional<OperationCorrelationId> correlationId,
		Optional<OperationCausationId> causationId)
{
	public MutationRequest(
			final DomainId domainId,
			final MutationPayload payload,
			final OperationSource source)
	{
		this(domainId, payload, source, OperationFamily.APPLICATION, Optional.empty(), Optional.empty());
	}

	public MutationRequest(
			final DomainId domainId,
			final MutationPayload payload,
			final OperationSource source,
			final Optional<OperationCorrelationId> correlationId,
			final Optional<OperationCausationId> causationId)
	{
		this(domainId, payload, source, OperationFamily.APPLICATION, correlationId, causationId);
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

	public Class<? extends ApplicationOperationPayload> payloadType()
	{
		return payload.getClass();
	}
}
