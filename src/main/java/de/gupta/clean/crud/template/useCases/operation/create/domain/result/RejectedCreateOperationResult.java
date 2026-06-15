package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationContext;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record RejectedCreateOperationResult<Model>(
		CreateOperationContext context,
		Collection<CreateOperationViolation> blockingViolations,
		Collection<CreateOperationViolation> toleratedViolations) implements CreateOperationResult<Model>
{
	public RejectedCreateOperationResult
	{
		Objects.requireNonNull(context, "context");
		blockingViolations = List.copyOf(Objects.requireNonNull(blockingViolations, "blockingViolations"));
		toleratedViolations = List.copyOf(Objects.requireNonNull(toleratedViolations, "toleratedViolations"));
	}

	@Override
	public CreateOperationStatus status()
	{
		return CreateOperationStatus.REJECTED;
	}
}