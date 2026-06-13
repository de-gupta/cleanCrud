package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationSource;

import java.util.Objects;
import java.util.Optional;

public record CreateApplicationResultContext(
		OperationSource source,
		String payloadTypeName,
		Optional<String> correlationId,
		Optional<String> causationId)
{
	public CreateApplicationResultContext
	{
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(payloadTypeName, "payloadTypeName");
		correlationId = Optional.ofNullable(correlationId).orElse(Optional.empty());
		causationId = Optional.ofNullable(causationId).orElse(Optional.empty());
	}
}
