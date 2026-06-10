package de.gupta.clean.crud.template.useCases.incantation.domain.handler;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.ApplicationIncantationPayload;

import java.util.*;

final class DefaultIncantationHandlerRegistry<DomainModelCreate>
		implements IncantationHandlerRegistry<DomainModelCreate>
{
	private final Map<Class<?>, RegisteredIncantationHandler<DomainModelCreate, ?>> handlersByType =
			new LinkedHashMap<>();

	@Override
	public Optional<RegisteredIncantationHandler<DomainModelCreate, ?>> findHandlerFor(
			final Class<? extends ApplicationIncantationPayload> payloadType)
	{
		return Optional.ofNullable(handlersByType.get(payloadType));
	}

	DefaultIncantationHandlerRegistry(final Collection<RegisteredIncantationHandler<DomainModelCreate, ?>> handlers)
	{
		Objects.requireNonNull(handlers, "handlers");
		for (var handler : handlers)
		{
			handlersByType.put(handler.payloadType(), handler);
		}
	}
}