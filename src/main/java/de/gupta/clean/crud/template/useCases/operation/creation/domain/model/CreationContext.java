package de.gupta.clean.crud.template.useCases.operation.creation.domain.model;

import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationFamily;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;

import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;

import java.util.Objects;
import java.util.Optional;

public record CreationContext<DomainId, DomainModel>(
		Optional<DomainId> domainId,
		OperationSource source,
		OperationFamily family,
		Class<?> payloadType,
		Optional<OperationCorrelationId> correlationId,
		Optional<OperationCausationId> causationId,
		Optional<DomainModel> beforeModel,
		Optional<DomainModel> afterModel)
{
	public CreationContext
	{
		Objects.requireNonNull(domainId, "domainId");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(family, "family");
		Objects.requireNonNull(payloadType, "payloadType");
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(causationId, "causationId");
		Objects.requireNonNull(beforeModel, "beforeModel");
		Objects.requireNonNull(afterModel, "afterModel");
	}

	public CreationContext<DomainId, DomainModel> withCreated(
			final DomainId createdId,
			final DomainModel createdModel)
	{
		return new CreationContext<>(
				Optional.of(createdId),
				source,
				family,
				payloadType,
				correlationId,
				causationId,
				beforeModel,
				Optional.of(createdModel));
	}
}
