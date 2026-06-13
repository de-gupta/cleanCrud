package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

import java.util.List;
import java.util.Objects;

public record CreatedCreationOperationResult<Model>(
		Model createdModel,
		List<CreationOperationViolation> toleratedViolations)
		implements CreationOperationResult<Model>
{
	public CreatedCreationOperationResult
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
