package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationContext;

import java.util.List;
import java.util.Optional;

public final class CreationOperationResults
{
	public static <Model> CreateOperationResult<Model> created(
			final CreationOperationContext context,
			final Model createdModel)
	{
		return created(context, createdModel, List.of());
	}

	public static <Model> CreateOperationResult<Model> created(
			final CreationOperationContext context,
			final Model createdModel,
			final List<CreationOperationViolation> toleratedViolations)
	{
		return new CreatedCreateOperationResult<>(context, createdModel, toleratedViolations);
	}

	public static <Model> CreateOperationResult<Model> rejected(final CreationOperationContext context)
	{
		return rejected(context, List.of());
	}

	public static <Model> CreateOperationResult<Model> rejected(
			final CreationOperationContext context,
			final List<CreationOperationViolation> blockingViolations)
	{
		return rejected(context, blockingViolations, List.of());
	}

	public static <Model> CreateOperationResult<Model> rejected(
			final CreationOperationContext context,
			final List<CreationOperationViolation> blockingViolations,
			final List<CreationOperationViolation> toleratedViolations)
	{
		return new RejectedCreateOperationResult<>(context, blockingViolations, toleratedViolations);
	}

	public static <Model> CreateOperationResult<Model> quarantined(final CreationOperationContext context)
	{
		return quarantined(context, List.of());
	}

	public static <Model> CreateOperationResult<Model> quarantined(
			final CreationOperationContext context,
			final List<CreationOperationViolation> blockingViolations)
	{
		return quarantined(context, blockingViolations, List.of(), Optional.empty());
	}

	public static <Model> CreateOperationResult<Model> quarantined(
			final CreationOperationContext context,
			final List<CreationOperationViolation> blockingViolations,
			final List<CreationOperationViolation> toleratedViolations)
	{
		return quarantined(context, blockingViolations, toleratedViolations, Optional.empty());
	}

	public static <Model> CreateOperationResult<Model> quarantined(
			final CreationOperationContext context,
			final List<CreationOperationViolation> blockingViolations,
			final List<CreationOperationViolation> toleratedViolations,
			final Optional<String> quarantineReference)
	{
		return new QuarantinedCreateOperationResult<>(
				context,
				blockingViolations,
				toleratedViolations,
				quarantineReference);
	}

	private CreationOperationResults()
	{
	}
}