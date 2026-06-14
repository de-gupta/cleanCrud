package de.gupta.clean.crud.template.useCases.operation.create.domain.handler;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class DefaultCreationHandlerRegistry<DomainCreateModel> implements CreationHandlerRegistry<DomainCreateModel>
{
	private final Map<Class<? extends CreateOperationPayload>, RegisteredCreationHandler<? extends CreateOperationPayload, DomainCreateModel>>
			handlersByPayloadType;

	@Override
	public <Payload extends CreateOperationPayload> RegisteredCreationHandler<Payload, DomainCreateModel> resolveHandlerFor(
			final Class<Payload> payloadType)
	{
		Objects.requireNonNull(payloadType, "payloadType");

		var exactMatch = handlersByPayloadType.get(payloadType);
		if (exactMatch != null)
		{
			return castHandler(exactMatch);
		}

		var assignableMatches = handlersByPayloadType.values()
		                                             .stream()
		                                             .filter(handler -> handler.supportsAssignable(payloadType))
		                                             .toList();
		return switch (assignableMatches.size())
		{
			case 0 -> throw new IllegalStateException(
					"No creation handler registered for payload type " + payloadType.getName());
			case 1 -> castHandler(assignableMatches.getFirst());
			default -> throw new IllegalStateException(
					"Multiple creation handlers match payload type "
							+ payloadType.getName()
							+ ": "
							+ handlerTypeNames(assignableMatches));
		};
	}

	@SuppressWarnings("unchecked")
	private <Payload extends CreateOperationPayload> RegisteredCreationHandler<Payload, DomainCreateModel> castHandler(
			final RegisteredCreationHandler<? extends CreateOperationPayload, DomainCreateModel> handler)
	{
		return (RegisteredCreationHandler<Payload, DomainCreateModel>) handler;
	}

	private String handlerTypeNames(
			final Collection<RegisteredCreationHandler<? extends CreateOperationPayload, DomainCreateModel>> handlers)
	{
		return handlers.stream()
		               .map(handler -> handler.payloadType().getName())
		               .toList()
		               .toString();
	}

	DefaultCreationHandlerRegistry(
			final Collection<? extends RegisteredCreationHandler<? extends CreateOperationPayload, DomainCreateModel>> handlers)
	{
		Objects.requireNonNull(handlers, "handlers");
		this.handlersByPayloadType = new LinkedHashMap<>();
		for (var handler : handlers)
		{
			var previous = handlersByPayloadType.putIfAbsent(handler.payloadType(), handler);
			if (previous != null)
			{
				throw new IllegalArgumentException(
						"Duplicate creation handler registration for payload type " + handler.payloadType().getName());
			}
		}
	}
}