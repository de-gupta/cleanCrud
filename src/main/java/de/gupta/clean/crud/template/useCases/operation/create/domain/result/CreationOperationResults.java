package de.gupta.clean.crud.template.useCases.operation.create.domain.result;

public final class CreationOperationResults
{
	public static <Model> CreationOperationResult created(final Model createdModel)
	{
		return new CreatedCreationOperationResult<>(createdModel);
	}

	public static CreationOperationResult rejected()
	{
		return new RejectedCreationOperationResult();
	}

	public static CreationOperationResult quarantined()
	{
		return new QuarantinedCreationOperationResult();
	}

	private CreationOperationResults()
	{
	}
}
