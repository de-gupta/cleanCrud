package de.gupta.clean.crud.template.useCases.incantation.domain.model;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.id.IncantationCausationId;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.id.IncantationCorrelationId;

import java.util.Objects;
import java.util.Optional;

public record IncantationContext<DomainId, DomainModel>(
		Optional<DomainId> domainId,
		IncantationSource source,
		IncantationFamily family,
		Class<?> payloadType,
		Optional<IncantationCorrelationId> correlationId,
		Optional<IncantationCausationId> causationId,
		Optional<DomainModel> beforeModel,
		Optional<DomainModel> afterModel)
{
	public IncantationContext
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

	public IncantationContext<DomainId, DomainModel> withCreated(
			final DomainId createdId,
			final DomainModel createdModel)
	{
		return new IncantationContext<>(
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
