package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationStatus;

import java.util.List;
import java.util.Objects;

public record RejectedCreateApplicationResult<Model>(
		List<CreateAPIViolation> blockingViolations,
		List<CreateAPIViolation> toleratedViolations)
		implements CreateApplicationResult<Model>
{
	public RejectedCreateApplicationResult
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