package de.gupta.clean.crud.template.useCases.operation.create.domain.model;

import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationRequestMetadata;
import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationSource;

import java.util.Objects;
import java.util.Optional;

public record CreationOperationContext(
		OperationSource source,
		String payloadTypeName,
		Optional<String> correlationId,
		Optional<String> causationId)
{
	public static CreationOperationContext from(final CreationOperationRequest<?> request)
	{
		return from(request.payload().getClass(), request.metadata());
	}

	public static CreationOperationContext from(
			final Class<?> payloadType,
			final OperationRequestMetadata metadata)
	{
		return new CreationOperationContext(
				metadata.source(),
				payloadType.getName(),
				metadata.correlationId().map(OperationCorrelationId::value),
				metadata.causationId().map(OperationCausationId::value));
	}

	public CreationOperationContext
	{
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(payloadTypeName, "payloadTypeName");
		correlationId = Optional.ofNullable(correlationId).orElse(Optional.empty());
		causationId = Optional.ofNullable(causationId).orElse(Optional.empty());
	}
}