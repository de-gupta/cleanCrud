package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.model;

import java.util.Objects;

public record CreateResult<DomainId, DomainModel>(
		DomainId domainId,
		DomainModel model)
{
	public static <DomainId, DomainModel> CreateResult<DomainId, DomainModel> of(
			final DomainId domainId,
			final DomainModel model)
	{
		return new CreateResult<>(domainId, model);
	}

	public CreateResult
	{
		Objects.requireNonNull(domainId, "domainId");
		Objects.requireNonNull(model, "model");
	}
}