package de.gupta.clean.crud.template.useCases.operation.create.domain.model;

import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationRequestMetadata;

import java.util.Objects;

public record CreationOperationRequest<Payload extends CreateOperationPayload>(
		Payload payload,
		OperationRequestMetadata metadata)
{
	public CreationOperationRequest
	{
		Objects.requireNonNull(payload, "payload");
		Objects.requireNonNull(metadata, "metadata");
	}

	public <MappedPayload extends CreateOperationPayload> CreationOperationRequest<MappedPayload> withPayload(
			final MappedPayload mappedPayload)
	{
		return new CreationOperationRequest<>(mappedPayload, metadata);
	}
}