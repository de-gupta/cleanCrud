package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record RejectedCreationOperationResult<Model>(
		Collection<CreationOperationViolation> blockingViolations,
		Collection<CreationOperationViolation> toleratedViolations) implements CreationOperationResult<Model>
{
	public RejectedCreationOperationResult
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