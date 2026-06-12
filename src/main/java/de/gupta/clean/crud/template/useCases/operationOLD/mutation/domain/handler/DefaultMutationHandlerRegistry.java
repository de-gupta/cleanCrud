package de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.handler;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;

import java.util.*;

final class DefaultMutationHandlerRegistry<DomainModel> implements MutationHandlerRegistry<DomainModel>
{
	private final Map<Class<? extends ApplicationOperationPayload>, RegisteredMutationHandler<DomainModel, ?>> handlers;

	@Override
	public Collection<RegisteredMutationHandler<DomainModel, ?>> handlers()
	{
		return List.copyOf(handlers.values());
	}

	@Override
	public Optional<RegisteredMutationHandler<DomainModel, ?>> findHandlerFor(
			final Class<? extends ApplicationOperationPayload> payloadType)
	{
		Objects.requireNonNull(payloadType, "payloadType");
		return Optional.ofNullable(handlers.get(payloadType));
	}

	DefaultMutationHandlerRegistry(final Collection<RegisteredMutationHandler<DomainModel, ?>> handlers)
	{
		Objects.requireNonNull(handlers, "handlers");
		var indexedHandlers = new LinkedHashMap<Class<? extends ApplicationOperationPayload>,
				RegisteredMutationHandler<DomainModel, ?>>();
		for (var registeredHandler : handlers)
		{
			var previous = indexedHandlers.put(registeredHandler.payloadType(), registeredHandler);
			if (previous != null)
			{
				throw new IllegalArgumentException(
						"Duplicate mutation handler registration for payload type " + registeredHandler.payloadType()
						                                                                               .getName());
			}
		}
		this.handlers = Map.copyOf(indexedHandlers);
	}
}