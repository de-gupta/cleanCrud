package de.gupta.clean.crud.template.useCases.operation.creation.application.service;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreateResult;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationResult;

@FunctionalInterface
public interface CreationService<DomainId, DomainModel>
{
	default CreateResult<DomainId, DomainModel> create(final CreationRequest<?> request)
	{
		return createWithResult(request).createdOrThrow();
	}

	CreationResult<DomainId, DomainModel> createWithResult(final CreationRequest<?> request);
}
