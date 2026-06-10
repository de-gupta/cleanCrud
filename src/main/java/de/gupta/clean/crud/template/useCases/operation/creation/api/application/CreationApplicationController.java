package de.gupta.clean.crud.template.useCases.operation.creation.api.application;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreateResult;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationResult;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;

import java.util.Optional;

@FunctionalInterface
public interface CreationApplicationController<DomainId, DomainModel>
{
	default CreateResult<DomainId, DomainModel> invoke(final CreationRequest<?> request)
	{
		return invokeWithResult(request).createdOrThrow();
	}

	CreationResult<DomainId, DomainModel> invokeWithResult(final CreationRequest<?> request);

	default <Payload extends ApplicationOperationPayload> CreateResult<DomainId, DomainModel> invoke(
			final Payload payload,
			final OperationSource source)
	{
		return invoke(new CreationRequest<>(payload, source));
	}

	default <Payload extends ApplicationOperationPayload> CreateResult<DomainId, DomainModel> invoke(
			final Payload payload,
			final OperationSource source,
			final Optional<OperationCorrelationId> correlationId,
			final Optional<OperationCausationId> causationId)
	{
		return invoke(new CreationRequest<>(payload, source, correlationId, causationId));
	}

	default <Payload extends ApplicationOperationPayload> CreateResult<DomainId, DomainModel> invokeUserIntent(
			final Payload payload)
	{
		return invoke(payload, OperationSource.USER_INTENT);
	}

	default <Payload extends ApplicationOperationPayload> CreateResult<DomainId, DomainModel> invokeInternalCommand(
			final Payload payload)
	{
		return invoke(payload, OperationSource.INTERNAL_COMMAND);
	}

	default <Payload extends ApplicationOperationPayload>
	CreateResult<DomainId, DomainModel> invokeAuthoritativeExternalEvent(final Payload payload)
	{
		return invoke(payload, OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);
	}

	default <Payload extends ApplicationOperationPayload> CreateResult<DomainId, DomainModel> invokeProcessEmittedAction(
			final Payload payload)
	{
		return invoke(payload, OperationSource.PROCESS_EMITTED_ACTION);
	}

	default <Payload extends ApplicationOperationPayload> CreationResult<DomainId, DomainModel> invokeWithResult(
			final Payload payload,
			final OperationSource source)
	{
		return invokeWithResult(new CreationRequest<>(payload, source));
	}

	default <Payload extends ApplicationOperationPayload> CreationResult<DomainId, DomainModel> invokeWithResult(
			final Payload payload,
			final OperationSource source,
			final Optional<OperationCorrelationId> correlationId,
			final Optional<OperationCausationId> causationId)
	{
		return invokeWithResult(new CreationRequest<>(payload, source, correlationId, causationId));
	}

	default <Payload extends ApplicationOperationPayload> CreationResult<DomainId, DomainModel>
	invokeUserIntentWithResult(final Payload payload)
	{
		return invokeWithResult(payload, OperationSource.USER_INTENT);
	}

	default <Payload extends ApplicationOperationPayload> CreationResult<DomainId, DomainModel>
	invokeInternalCommandWithResult(final Payload payload)
	{
		return invokeWithResult(payload, OperationSource.INTERNAL_COMMAND);
	}

	default <Payload extends ApplicationOperationPayload> CreationResult<DomainId, DomainModel>
	invokeAuthoritativeExternalEventWithResult(final Payload payload)
	{
		return invokeWithResult(payload, OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);
	}

	default <Payload extends ApplicationOperationPayload> CreationResult<DomainId, DomainModel>
	invokeProcessEmittedActionWithResult(final Payload payload)
	{
		return invokeWithResult(payload, OperationSource.PROCESS_EMITTED_ACTION);
	}
}