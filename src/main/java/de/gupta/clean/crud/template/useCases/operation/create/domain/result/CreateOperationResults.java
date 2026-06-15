package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationContext;

import java.util.List;
import java.util.Optional;

public enum CreateOperationResults
{
	;

	public static <Model> CreateOperationResult<Model> created(
			final CreateOperationContext context,
			final Model createdModel)
	{
		return created(context, createdModel, List.of());
	}

	public static <Model> CreateOperationResult<Model> created(
			final CreateOperationContext context,
			final Model createdModel,
			final List<CreateOperationViolation> toleratedViolations)
	{
		return new CreatedCreateOperationResult<>(context, createdModel, toleratedViolations);
	}

	public static <Model> CreateOperationResult<Model> rejected(final CreateOperationContext context)
	{
		return rejected(context, List.of());
	}

	public static <Model> CreateOperationResult<Model> rejected(
			final CreateOperationContext context,
			final List<CreateOperationViolation> blockingViolations)
	{
		return rejected(context, blockingViolations, List.of());
	}

	public static <Model> CreateOperationResult<Model> rejected(
			final CreateOperationContext context,
			final List<CreateOperationViolation> blockingViolations,
			final List<CreateOperationViolation> toleratedViolations)
	{
		return new RejectedCreateOperationResult<>(context, blockingViolations, toleratedViolations);
	}

	public static <Model> CreateOperationResult<Model> quarantined(final CreateOperationContext context)
	{
		return quarantined(context, List.of());
	}

	public static <Model> CreateOperationResult<Model> quarantined(
			final CreateOperationContext context,
			final List<CreateOperationViolation> blockingViolations)
	{
		return quarantined(context, blockingViolations, List.of(), Optional.empty());
	}

	public static <Model> CreateOperationResult<Model> quarantined(
			final CreateOperationContext context,
			final List<CreateOperationViolation> blockingViolations,
			final List<CreateOperationViolation> toleratedViolations,
			final Optional<String> quarantineReference)
	{
		return new QuarantinedCreateOperationResult<>(
				context,
				blockingViolations,
				toleratedViolations,
				quarantineReference);
	}

	public static <Model> CreateOperationResult<Model> quarantined(
			final CreateOperationContext context,
			final List<CreateOperationViolation> blockingViolations,
			final List<CreateOperationViolation> toleratedViolations)
	{
		return quarantined(context, blockingViolations, toleratedViolations, Optional.empty());
	}

}