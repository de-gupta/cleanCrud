package de.gupta.clean.crud.template.useCases.incantation.application.service;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.CreateResult;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationRequest;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationResult;

@FunctionalInterface
public interface IncantationService<DomainId, DomainModel>
{
	default CreateResult<DomainId, DomainModel> incant(final IncantationRequest<?> request)
	{
		return incantWithResult(request).createdOrThrow();
	}

	IncantationResult<DomainId, DomainModel> incantWithResult(final IncantationRequest<?> request);
}
