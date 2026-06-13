package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationStatus;

import java.util.List;
import java.util.Objects;

public record CreatedCreateAPIResult<Model>(
		Model createdModel,
		List<CreateAPIViolation> toleratedViolations)
		implements CreateAPIResult<Model>
{
	public CreatedCreateAPIResult
	{
		Objects.requireNonNull(createdModel, "createdModel");
		toleratedViolations = List.copyOf(Objects.requireNonNull(toleratedViolations, "toleratedViolations"));
	}

	@Override
	public CreationOperationStatus status()
	{
		return CreationOperationStatus.CREATED;
	}
}
