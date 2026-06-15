package de.gupta.clean.crud.template.useCases.operation.create.domain.handler;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;

import java.util.Collection;

public interface CreationHandlerRegistry<DomainModel>
{
	static <DomainModel> CreationHandlerRegistry<DomainModel> of(
			final Collection<? extends RegisteredCreationHandler<? extends CreateOperationPayload, DomainModel>> handlers)
	{
		return new DefaultCreationHandlerRegistry<>(handlers);
	}

	<DomainCreatePayload extends CreateOperationPayload> RegisteredCreationHandler<DomainCreatePayload, DomainModel> resolveHandlerFor(
			Class<DomainCreatePayload> payloadType);
}