package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreateOperationStatus;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record CreatedCreateApplicationResult<Model>(
		CreateApplicationResultContext context,
		Model createdModel,
		Collection<CreateApplicationViolation> toleratedViolations)
		implements CreateApplicationResult<Model>
{
	public CreatedCreateApplicationResult
	{
		Objects.requireNonNull(context, "context");
		Objects.requireNonNull(createdModel, "createdModel");
		toleratedViolations = List.copyOf(Objects.requireNonNull(toleratedViolations, "toleratedViolations"));
	}

	@Override
	public CreateOperationStatus status()
	{
		return CreateOperationStatus.CREATED;
	}
}