package de.gupta.clean.crud.template.useCases.operationOLD.creation.application.service;

import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.model.CreationRequest;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.model.CreationResult;

@FunctionalInterface
public interface CreationService<DomainId, DomainModel>
{
	default CreationResult<DomainId, DomainModel> create(final CreationRequest<?> request)
	{
		return createWithResult(request);
	}

	CreationResult<DomainId, DomainModel> createWithResult(final CreationRequest<?> request);
}