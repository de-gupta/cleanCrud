package de.gupta.clean.crud.template.useCases.mutation.domain.model;

import java.util.Objects;

public record MutationRequest<DomainId, MutationPayload extends ApplicationMutationPayload>(
		DomainId domainId,
		MutationPayload payload,
		MutationSource source)
{
	public MutationRequest
	{
		Objects.requireNonNull(domainId, "domainId");
		Objects.requireNonNull(payload, "payload");
		Objects.requireNonNull(source, "source");
	}
}
