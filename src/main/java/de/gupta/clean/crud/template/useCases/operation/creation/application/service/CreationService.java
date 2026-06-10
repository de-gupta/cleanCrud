package de.gupta.clean.crud.template.useCases.operation.creation.application.service;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreateResult;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.model.CreationResult;

@FunctionalInterface
public interface CreationService<DomainId, DomainModel>
{
	default CreateResult<DomainId, DomainModel> incant(final CreationRequest<?> request)
	{
		return incantWithResult(request).createdOrThrow();
	}

	CreationResult<DomainId, DomainModel> incantWithResult(final CreationRequest<?> request);
}
