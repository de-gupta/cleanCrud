package de.gupta.clean.crud.template.useCases.operation.create.domain.handler;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class DefaultCreationHandlerRegistry<DomainModel> implements CreationHandlerRegistry<DomainModel>
{
	private final Map<Class<? extends CreateOperationPayload>, RegisteredCreationHandler<? extends CreateOperationPayload, DomainModel>>
			handlersByPayloadType;

	@Override
	public <DomainCreatePayload extends CreateOperationPayload> RegisteredCreationHandler<DomainCreatePayload, DomainModel> resolveHandlerFor(
			final Class<DomainCreatePayload> payloadType)
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
	private <Payload extends CreateOperationPayload> RegisteredCreationHandler<Payload, DomainModel> castHandler(
			final RegisteredCreationHandler<? extends CreateOperationPayload, DomainModel> handler)
	{
		return (RegisteredCreationHandler<Payload, DomainModel>) handler;
	}

	private String handlerTypeNames(
			final Collection<RegisteredCreationHandler<? extends CreateOperationPayload, DomainModel>> handlers)
	{
		return handlers.stream()
		               .map(handler -> handler.payloadType().getName())
		               .toList()
		               .toString();
	}

	DefaultCreationHandlerRegistry(
			final Collection<? extends RegisteredCreationHandler<? extends CreateOperationPayload, DomainModel>> handlers)
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