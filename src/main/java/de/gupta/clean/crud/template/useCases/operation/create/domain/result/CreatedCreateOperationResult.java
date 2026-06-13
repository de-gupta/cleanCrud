package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationContext;

import java.util.List;
import java.util.Objects;

public record CreatedCreateOperationResult<Model>(
		CreationOperationContext context,
		Model createdModel,
		List<CreationOperationViolation> toleratedViolations)
		implements CreateOperationResult<Model>
{
	public CreatedCreateOperationResult
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