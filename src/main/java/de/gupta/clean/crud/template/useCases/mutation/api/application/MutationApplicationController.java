package de.gupta.clean.crud.template.useCases.mutation.api.application;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.ApplicationMutationPayload;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationRequest;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.id.MutationCausationId;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.id.MutationCorrelationId;

import java.util.Optional;

@FunctionalInterface
public interface MutationApplicationController<DomainId, DomainModel>
{
	IdentifiedModel<DomainId, DomainModel> apply(final MutationRequest<DomainId, ?> request);

	default <Payload extends ApplicationMutationPayload> IdentifiedModel<DomainId, DomainModel> apply(
			final DomainId id,
			final Payload payload,
			final MutationSource source)
	{
		return apply(new MutationRequest<>(id, payload, source));
	}

	default <Payload extends ApplicationMutationPayload> IdentifiedModel<DomainId, DomainModel> apply(
			final DomainId id,
			final Payload payload,
			final MutationSource source,
			final Optional<MutationCorrelationId> correlationId,
			final Optional<MutationCausationId> causationId)
	{
		return apply(new MutationRequest<>(id, payload, source, correlationId, causationId));
	}

	default <Payload extends ApplicationMutationPayload> IdentifiedModel<DomainId, DomainModel> applyUserIntent(
			final DomainId id,
			final Payload payload)
	{
		return apply(id, payload, MutationSource.USER_INTENT);
	}

	default <Payload extends ApplicationMutationPayload> IdentifiedModel<DomainId, DomainModel> applyInternalCommand(
			final DomainId id,
			final Payload payload)
	{
		return apply(id, payload, MutationSource.INTERNAL_COMMAND);
	}

	default <Payload extends ApplicationMutationPayload> IdentifiedModel<DomainId, DomainModel>
	applyAuthoritativeExternalEvent(
			final DomainId id,
			final Payload payload)
	{
		return apply(id, payload, MutationSource.AUTHORITATIVE_EXTERNAL_EVENT);
	}

	default <Payload extends ApplicationMutationPayload> IdentifiedModel<DomainId, DomainModel> applyProcessEmittedAction(
			final DomainId id,
			final Payload payload)
	{
		return apply(id, payload, MutationSource.PROCESS_EMITTED_ACTION);
	}
}