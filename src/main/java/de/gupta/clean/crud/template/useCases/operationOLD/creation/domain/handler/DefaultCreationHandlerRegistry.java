package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.handler;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;

import java.util.*;

final class DefaultCreationHandlerRegistry<DomainModelCreate>
		implements CreationHandlerRegistry<DomainModelCreate>
{
	private final Map<Class<?>, RegisteredCreationHandler<DomainModelCreate, ?>> handlersByType =
			new LinkedHashMap<>();

	@Override
	public Optional<RegisteredCreationHandler<DomainModelCreate, ?>> findHandlerFor(
			final Class<? extends ApplicationOperationPayload> payloadType)
	{
		return Optional.ofNullable(handlersByType.get(payloadType));
	}

	DefaultCreationHandlerRegistry(final Collection<RegisteredCreationHandler<DomainModelCreate, ?>> handlers)
	{
		Objects.requireNonNull(handlers, "handlers");
		for (var handler : handlers)
		{
			handlersByType.put(handler.payloadType(), handler);
		}
	}
}