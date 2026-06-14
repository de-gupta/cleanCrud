package de.gupta.clean.crud.template.useCases.operation.create.domain.handler;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;

import java.util.Collection;

public interface CreationHandlerRegistry<DomainCreateModel>
{
	static <DomainCreateModel> CreationHandlerRegistry<DomainCreateModel> of(
			final Collection<? extends RegisteredCreationHandler<? extends CreateOperationPayload, DomainCreateModel>> handlers)
	{
		return new DefaultCreationHandlerRegistry<>(handlers);
	}

	<Payload extends CreateOperationPayload> RegisteredCreationHandler<Payload, DomainCreateModel> resolveHandlerFor(
			Class<Payload> payloadType);
}