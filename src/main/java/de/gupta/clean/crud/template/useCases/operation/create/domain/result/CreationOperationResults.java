package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

public final class CreationOperationResults
{
	public static <Model> CreationOperationResult<Model> created(final Model createdModel)
	{
		return new CreatedCreationOperationResult<>(createdModel);
	}

	public static <Model> CreationOperationResult<Model> rejected()
	{
		return new RejectedCreationOperationResult<>();
	}

	public static <Model> CreationOperationResult<Model> quarantined()
	{
		return new QuarantinedCreationOperationResult<>();
	}

	private CreationOperationResults()
	{
	}
}