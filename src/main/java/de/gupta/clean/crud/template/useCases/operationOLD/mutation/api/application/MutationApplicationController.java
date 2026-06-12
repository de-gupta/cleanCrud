package de.gupta.clean.crud.template.useCases.operationOLD.mutation.api.application;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.id.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.model.MutationResult;

import java.util.Optional;

@FunctionalInterface
public interface MutationApplicationController<DomainId, DomainModel>
{
	default MutationResult<DomainId, DomainModel> mutate(final MutationRequest<DomainId, ?> request)
	{
		return mutateWithResult(request);
	}

	MutationResult<DomainId, DomainModel> mutateWithResult(final MutationRequest<DomainId, ?> request);

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel> mutate(
			final DomainId id,
			final Payload payload,
			final OperationSource source)
	{
		return mutate(new MutationRequest<>(id, payload, source));
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel> mutate(
			final DomainId id,
			final Payload payload,
			final OperationSource source,
			final Optional<OperationCorrelationId> correlationId,
			final Optional<OperationCausationId> causationId)
	{
		return mutate(new MutationRequest<>(id, payload, source, correlationId, causationId));
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel> mutateUserIntent(
			final DomainId id,
			final Payload payload)
	{
		return mutate(id, payload, OperationSource.USER_INTENT);
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel> mutateInternalCommand(
			final DomainId id,
			final Payload payload)
	{
		return mutate(id, payload, OperationSource.INTERNAL_COMMAND);
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel>
	mutateAuthoritativeExternalEvent(
			final DomainId id,
			final Payload payload)
	{
		return mutate(id, payload, OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel> mutateProcessEmittedAction(
			final DomainId id,
			final Payload payload)
	{
		return mutate(id, payload, OperationSource.PROCESS_EMITTED_ACTION);
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel> mutateWithResult(
			final DomainId id,
			final Payload payload,
			final OperationSource source)
	{
		return mutateWithResult(new MutationRequest<>(id, payload, source));
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel> mutateWithResult(
			final DomainId id,
			final Payload payload,
			final OperationSource source,
			final Optional<OperationCorrelationId> correlationId,
			final Optional<OperationCausationId> causationId)
	{
		return mutateWithResult(new MutationRequest<>(id, payload, source, correlationId, causationId));
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel> mutateUserIntentWithResult(
			final DomainId id,
			final Payload payload)
	{
		return mutateWithResult(id, payload, OperationSource.USER_INTENT);
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel>
	mutateInternalCommandWithResult(
			final DomainId id,
			final Payload payload)
	{
		return mutateWithResult(id, payload, OperationSource.INTERNAL_COMMAND);
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel>
	mutateAuthoritativeExternalEventWithResult(
			final DomainId id,
			final Payload payload)
	{
		return mutateWithResult(id, payload, OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel>
	mutateProcessEmittedActionWithResult(
			final DomainId id,
			final Payload payload)
	{
		return mutateWithResult(id, payload, OperationSource.PROCESS_EMITTED_ACTION);
	}
}