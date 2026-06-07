package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

@FunctionalInterface
public interface CrudWorkflow<Result>
{
	Result inTransaction();

	default void afterTransaction(final Result result)
	{
	}

	default boolean readOnly()
	{
		return false;
	}
}