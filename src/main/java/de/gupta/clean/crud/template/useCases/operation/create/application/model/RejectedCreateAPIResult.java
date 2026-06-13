package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationStatus;

import java.util.List;
import java.util.Objects;

public record RejectedCreateAPIResult<Model>(
		List<CreateAPIViolation> blockingViolations,
		List<CreateAPIViolation> toleratedViolations)
		implements CreateAPIResult<Model>
{
	public RejectedCreateAPIResult
	{
		blockingViolations = List.copyOf(Objects.requireNonNull(blockingViolations, "blockingViolations"));
		toleratedViolations = List.copyOf(Objects.requireNonNull(toleratedViolations, "toleratedViolations"));
	}

	@Override
	public CreationOperationStatus status()
	{
		return CreationOperationStatus.REJECTED;
	}
}
