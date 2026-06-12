package de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.handler;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;

import java.util.Collection;
import java.util.Optional;

public interface MutationHandlerRegistry<DomainModel>
{
	static <DomainModel> MutationHandlerRegistry<DomainModel> of(
			final Collection<RegisteredMutationHandler<DomainModel, ?>> handlers)
	{
		return new DefaultMutationHandlerRegistry<>(handlers);
	}

	Collection<RegisteredMutationHandler<DomainModel, ?>> handlers();

	Optional<RegisteredMutationHandler<DomainModel, ?>> findHandlerFor(
			final Class<? extends ApplicationOperationPayload> payloadType);
}