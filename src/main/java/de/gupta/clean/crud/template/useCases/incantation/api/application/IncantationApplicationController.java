package de.gupta.clean.crud.template.useCases.incantation.api.application;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.*;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.id.IncantationCausationId;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.id.IncantationCorrelationId;

import java.util.Optional;

@FunctionalInterface
public interface IncantationApplicationController<DomainId, DomainModel>
{
	default CreateResult<DomainId, DomainModel> invoke(final IncantationRequest<?> request)
	{
		return invokeWithResult(request).createdOrThrow();
	}

	IncantationResult<DomainId, DomainModel> invokeWithResult(final IncantationRequest<?> request);

	default <Payload extends ApplicationIncantationPayload> CreateResult<DomainId, DomainModel> invoke(
			final Payload payload,
			final IncantationSource source)
	{
		return invoke(new IncantationRequest<>(payload, source));
	}

	default <Payload extends ApplicationIncantationPayload> CreateResult<DomainId, DomainModel> invoke(
			final Payload payload,
			final IncantationSource source,
			final Optional<IncantationCorrelationId> correlationId,
			final Optional<IncantationCausationId> causationId)
	{
		return invoke(new IncantationRequest<>(payload, source, correlationId, causationId));
	}

	default <Payload extends ApplicationIncantationPayload> CreateResult<DomainId, DomainModel> invokeUserIntent(
			final Payload payload)
	{
		return invoke(payload, IncantationSource.USER_INTENT);
	}

	default <Payload extends ApplicationIncantationPayload> CreateResult<DomainId, DomainModel> invokeInternalCommand(
			final Payload payload)
	{
		return invoke(payload, IncantationSource.INTERNAL_COMMAND);
	}

	default <Payload extends ApplicationIncantationPayload>
	CreateResult<DomainId, DomainModel> invokeAuthoritativeExternalEvent(final Payload payload)
	{
		return invoke(payload, IncantationSource.AUTHORITATIVE_EXTERNAL_EVENT);
	}

	default <Payload extends ApplicationIncantationPayload> CreateResult<DomainId, DomainModel> invokeProcessEmittedAction(
			final Payload payload)
	{
		return invoke(payload, IncantationSource.PROCESS_EMITTED_ACTION);
	}

	default <Payload extends ApplicationIncantationPayload> IncantationResult<DomainId, DomainModel> invokeWithResult(
			final Payload payload,
			final IncantationSource source)
	{
		return invokeWithResult(new IncantationRequest<>(payload, source));
	}

	default <Payload extends ApplicationIncantationPayload> IncantationResult<DomainId, DomainModel> invokeWithResult(
			final Payload payload,
			final IncantationSource source,
			final Optional<IncantationCorrelationId> correlationId,
			final Optional<IncantationCausationId> causationId)
	{
		return invokeWithResult(new IncantationRequest<>(payload, source, correlationId, causationId));
	}

	default <Payload extends ApplicationIncantationPayload> IncantationResult<DomainId, DomainModel>
	invokeUserIntentWithResult(final Payload payload)
	{
		return invokeWithResult(payload, IncantationSource.USER_INTENT);
	}

	default <Payload extends ApplicationIncantationPayload> IncantationResult<DomainId, DomainModel>
	invokeInternalCommandWithResult(final Payload payload)
	{
		return invokeWithResult(payload, IncantationSource.INTERNAL_COMMAND);
	}

	default <Payload extends ApplicationIncantationPayload> IncantationResult<DomainId, DomainModel>
	invokeAuthoritativeExternalEventWithResult(final Payload payload)
	{
		return invokeWithResult(payload, IncantationSource.AUTHORITATIVE_EXTERNAL_EVENT);
	}

	default <Payload extends ApplicationIncantationPayload> IncantationResult<DomainId, DomainModel>
	invokeProcessEmittedActionWithResult(final Payload payload)
	{
		return invokeWithResult(payload, IncantationSource.PROCESS_EMITTED_ACTION);
	}
}