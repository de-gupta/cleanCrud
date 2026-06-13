package de.gupta.clean.crud.template.useCases.operation.create.application.model;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreateOperationStatus;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record RejectedCreateApplicationResult<Model>(
		CreateApplicationResultContext context,
		Collection<CreateApplicationViolation> blockingViolations,
		Collection<CreateApplicationViolation> toleratedViolations)
		implements CreateApplicationResult<Model>
{
	public RejectedCreateApplicationResult
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