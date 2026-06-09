package de.gupta.clean.crud.template.useCases.mutation.domain.handler;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.ApplicationMutationPayload;

@FunctionalInterface
public interface MutationHandler<DomainModel, MutationPayload extends ApplicationMutationPayload>
{
	DomainModel apply(final DomainModel currentModel, final MutationPayload payload);
}
