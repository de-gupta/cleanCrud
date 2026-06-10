package de.gupta.clean.crud.template.useCases.operation.creation.domain.handler;

import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;

@FunctionalInterface
public interface CreationHandler<DomainModelCreate, CreationPayload extends ApplicationOperationPayload>
{
	DomainModelCreate apply(final CreationPayload payload);
}
