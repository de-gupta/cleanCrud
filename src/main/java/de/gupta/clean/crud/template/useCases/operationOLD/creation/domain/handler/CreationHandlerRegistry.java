package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.handler;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.ApplicationOperationPayload;

import java.util.Collection;
import java.util.Optional;

public interface CreationHandlerRegistry<DomainModelCreate>
{
	static <DomainModelCreate> CreationHandlerRegistry<DomainModelCreate> of(
			final Collection<RegisteredCreationHandler<DomainModelCreate, ?>> handlers)
	{
		return new DefaultCreationHandlerRegistry<>(handlers);
	}

	Optional<RegisteredCreationHandler<DomainModelCreate, ?>> findHandlerFor(
			Class<? extends ApplicationOperationPayload> payloadType);
}