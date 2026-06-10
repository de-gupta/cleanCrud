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
	default CreateResult<DomainId, DomainModel> create(final CreationRequest<?> request)
	{
		return createWithResult(request).createdOrThrow();
	}

	CreationResult<DomainId, DomainModel> createWithResult(final CreationRequest<?> request);

	default <Payload extends ApplicationOperationPayload> CreateResult<DomainId, DomainModel> create(
			final Payload payload,
			final OperationSource source)
	{
		return create(new CreationRequest<>(payload, source));
	}

	default <Payload extends ApplicationOperationPayload> CreateResult<DomainId, DomainModel> create(
			final Payload payload,
			final OperationSource source,
			final Optional<OperationCorrelationId> correlationId,
			final Optional<OperationCausationId> causationId)
	{
		return create(new CreationRequest<>(payload, source, correlationId, causationId));
	}

	default <Payload extends ApplicationOperationPayload> CreateResult<DomainId, DomainModel> createUserIntent(
			final Payload payload)
	{
		return create(payload, OperationSource.USER_INTENT);
	}

	default <Payload extends ApplicationOperationPayload> CreateResult<DomainId, DomainModel> createInternalCommand(
			final Payload payload)
	{
		return create(payload, OperationSource.INTERNAL_COMMAND);
	}

	default <Payload extends ApplicationOperationPayload>
	CreateResult<DomainId, DomainModel> createAuthoritativeExternalEvent(final Payload payload)
	{
		return create(payload, OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);
	}

	default <Payload extends ApplicationOperationPayload> CreateResult<DomainId, DomainModel> createProcessEmittedAction(
			final Payload payload)
	{
		return create(payload, OperationSource.PROCESS_EMITTED_ACTION);
	}

	default <Payload extends ApplicationOperationPayload> CreationResult<DomainId, DomainModel> createWithResult(
			final Payload payload,
			final OperationSource source)
	{
		return createWithResult(new CreationRequest<>(payload, source));
	}

	default <Payload extends ApplicationOperationPayload> CreationResult<DomainId, DomainModel> createWithResult(
			final Payload payload,
			final OperationSource source,
			final Optional<OperationCorrelationId> correlationId,
			final Optional<OperationCausationId> causationId)
	{
		return createWithResult(new CreationRequest<>(payload, source, correlationId, causationId));
	}

	default <Payload extends ApplicationOperationPayload> CreationResult<DomainId, DomainModel>
	createUserIntentWithResult(final Payload payload)
	{
		return createWithResult(payload, OperationSource.USER_INTENT);
	}

	default <Payload extends ApplicationOperationPayload> CreationResult<DomainId, DomainModel>
	createInternalCommandWithResult(final Payload payload)
	{
		return createWithResult(payload, OperationSource.INTERNAL_COMMAND);
	}

	default <Payload extends ApplicationOperationPayload> CreationResult<DomainId, DomainModel>
	createAuthoritativeExternalEventWithResult(final Payload payload)
	{
		return createWithResult(payload, OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);
	}

	default <Payload extends ApplicationOperationPayload> CreationResult<DomainId, DomainModel>
	createProcessEmittedActionWithResult(final Payload payload)
	{
		return createWithResult(payload, OperationSource.PROCESS_EMITTED_ACTION);
	}
}