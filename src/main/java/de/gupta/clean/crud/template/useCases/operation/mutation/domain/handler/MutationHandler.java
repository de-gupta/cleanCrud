package de.gupta.clean.crud.template.useCases.operation.mutation.domain.handler;

import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;

@FunctionalInterface
public interface MutationHandler<DomainModel, MutationPayload extends ApplicationOperationPayload>
{
	DomainModel apply(final DomainModel currentModel, final MutationPayload payload);
}
