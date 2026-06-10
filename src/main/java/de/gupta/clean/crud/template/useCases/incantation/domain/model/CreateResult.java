package de.gupta.clean.crud.template.useCases.incantation.domain.model;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;

import java.util.Objects;

public record CreateResult<DomainId, DomainModel>(IdentifiedModel<DomainId, DomainModel> created)
{
	public CreateResult
	{
		Objects.requireNonNull(created, "created");
	}

	public DomainId domainId()
	{
		return created.id();
	}

	public DomainModel model()
	{
		return created.model();
	}
}
