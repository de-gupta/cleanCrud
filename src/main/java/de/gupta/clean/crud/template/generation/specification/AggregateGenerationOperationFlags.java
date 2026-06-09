package de.gupta.clean.crud.template.generation.specification;

public record AggregateGenerationOperationFlags(
		boolean save,
		boolean update,
		boolean delete)
{
	public static AggregateGenerationOperationFlags disabled()
	{
		return new AggregateGenerationOperationFlags(false, false, false);
	}

	public boolean anyEnabled()
	{
		return save || update || delete;
	}
}
