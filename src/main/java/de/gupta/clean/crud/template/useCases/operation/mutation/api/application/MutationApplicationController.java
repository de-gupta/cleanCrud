package de.gupta.clean.crud.template.useCases.operation.mutation.api.application;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.operation.domain.model.ApplicationOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCausationId;
import de.gupta.clean.crud.template.useCases.operation.domain.model.id.OperationCorrelationId;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.model.MutationResult;

import java.util.Optional;

@FunctionalInterface
public interface MutationApplicationController<DomainId, DomainModel>
{
	default IdentifiedModel<DomainId, DomainModel> apply(final MutationRequest<DomainId, ?> request)
	{
		return applyWithResult(request).updatedOrThrow();
	}

	MutationResult<DomainId, DomainModel> applyWithResult(final MutationRequest<DomainId, ?> request);

	default <Payload extends ApplicationOperationPayload> IdentifiedModel<DomainId, DomainModel> apply(
			final DomainId id,
			final Payload payload,
			final OperationSource source)
	{
		return apply(new MutationRequest<>(id, payload, source));
	}

	default <Payload extends ApplicationOperationPayload> IdentifiedModel<DomainId, DomainModel> apply(
			final DomainId id,
			final Payload payload,
			final OperationSource source,
			final Optional<OperationCorrelationId> correlationId,
			final Optional<OperationCausationId> causationId)
	{
		return apply(new MutationRequest<>(id, payload, source, correlationId, causationId));
	}

	default <Payload extends ApplicationOperationPayload> IdentifiedModel<DomainId, DomainModel> applyUserIntent(
			final DomainId id,
			final Payload payload)
	{
		return apply(id, payload, OperationSource.USER_INTENT);
	}

	default <Payload extends ApplicationOperationPayload> IdentifiedModel<DomainId, DomainModel> applyInternalCommand(
			final DomainId id,
			final Payload payload)
	{
		return apply(id, payload, OperationSource.INTERNAL_COMMAND);
	}

	default <Payload extends ApplicationOperationPayload> IdentifiedModel<DomainId, DomainModel>
	applyAuthoritativeExternalEvent(
			final DomainId id,
			final Payload payload)
	{
		return apply(id, payload, OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);
	}

	default <Payload extends ApplicationOperationPayload> IdentifiedModel<DomainId, DomainModel> applyProcessEmittedAction(
			final DomainId id,
			final Payload payload)
	{
		return apply(id, payload, OperationSource.PROCESS_EMITTED_ACTION);
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel> applyWithResult(
			final DomainId id,
			final Payload payload,
			final OperationSource source)
	{
		return applyWithResult(new MutationRequest<>(id, payload, source));
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel> applyWithResult(
			final DomainId id,
			final Payload payload,
			final OperationSource source,
			final Optional<OperationCorrelationId> correlationId,
			final Optional<OperationCausationId> causationId)
	{
		return applyWithResult(new MutationRequest<>(id, payload, source, correlationId, causationId));
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel> applyUserIntentWithResult(
			final DomainId id,
			final Payload payload)
	{
		return applyWithResult(id, payload, OperationSource.USER_INTENT);
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel>
	applyInternalCommandWithResult(
			final DomainId id,
			final Payload payload)
	{
		return applyWithResult(id, payload, OperationSource.INTERNAL_COMMAND);
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel>
	applyAuthoritativeExternalEventWithResult(
			final DomainId id,
			final Payload payload)
	{
		return applyWithResult(id, payload, OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);
	}

	default <Payload extends ApplicationOperationPayload> MutationResult<DomainId, DomainModel>
	applyProcessEmittedActionWithResult(
			final DomainId id,
			final Payload payload)
	{
		return applyWithResult(id, payload, OperationSource.PROCESS_EMITTED_ACTION);
	}
}