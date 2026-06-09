package de.gupta.clean.crud.template.useCases.mutation.domain.model;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.id.MutationCausationId;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.id.MutationCorrelationId;

import java.util.Objects;
import java.util.Optional;

public record MutationContext<DomainId, DomainModel>(
		DomainId domainId,
		MutationSource source,
		MutationFamily family,
		Class<? extends ApplicationMutationPayload> payloadType,
		Optional<MutationCorrelationId> correlationId,
		Optional<MutationCausationId> causationId,
		Optional<DomainModel> beforeModel,
		Optional<DomainModel> afterModel)
{
	public MutationContext
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
}
