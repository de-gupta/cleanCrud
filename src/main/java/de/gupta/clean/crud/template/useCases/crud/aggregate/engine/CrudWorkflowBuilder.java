package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CrudWorkflowBuilder<Result>
{
	private final Supplier<Result> transactionalAction;
	private Consumer<Result> afterTransaction = _ ->
	{
	};
	private boolean readOnly;

	public static <Result> CrudWorkflowBuilder<Result> writeFlow(final Supplier<Result> transactionalAction)
	{
		return new CrudWorkflowBuilder<>(transactionalAction);
	}

	public static <Result> CrudWorkflowBuilder<Result> readOnlyFlow(final Supplier<Result> transactionalAction)
	{
		return new CrudWorkflowBuilder<>(transactionalAction).readOnly();
	}

	public CrudWorkflowBuilder<Result> afterTransaction(final Consumer<Result> afterTransaction)
	{
		this.afterTransaction = afterTransaction;
		return this;
	}

	public CrudWorkflowBuilder<Result> readOnly()
	{
		this.readOnly = true;
		return this;
	}

	public CrudWorkflow<Result> build()
	{
		return new CrudWorkflow<>()
		{
			@Override
			public Result inTransaction()
			{
				return transactionalAction.get();
			}

			@Override
			public void afterTransaction(final Result result)
			{
				afterTransaction.accept(result);
			}

			@Override
			public boolean readOnly()
			{
				return readOnly;
			}
		};
	}

	private CrudWorkflowBuilder(final Supplier<Result> transactionalAction)
	{
		this.transactionalAction = transactionalAction;
	}
}