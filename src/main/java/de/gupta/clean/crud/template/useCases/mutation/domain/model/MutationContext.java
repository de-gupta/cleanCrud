package de.gupta.clean.crud.template.useCases.mutation.domain.model;

import java.util.Objects;
import java.util.Optional;

public record MutationContext<DomainId, DomainModel>(
		DomainId domainId,
		MutationSource source,
		Optional<DomainModel> beforeModel,
		Optional<DomainModel> afterModel)
{
	public MutationContext
	{
		Objects.requireNonNull(domainId, "domainId");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(beforeModel, "beforeModel");
		Objects.requireNonNull(afterModel, "afterModel");
	}
}
