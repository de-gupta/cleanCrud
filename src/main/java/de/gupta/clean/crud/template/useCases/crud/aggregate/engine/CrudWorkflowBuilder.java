package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class CrudWorkflowBuilder<Result>
{
	private final Supplier<Result> transactionalAction;
	private Consumer<Result> afterTransaction = _ ->
	{
	};
	private Function<Result, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests = _ -> List.of();
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

	public CrudWorkflowBuilder<Result> startDurableProcesses(
			final Function<Result, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		this.durableProcessStartRequests = durableProcessStartRequests;
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

			@Override
			public Collection<DurableProcessStartRequest<?, ?>> durableProcessStartRequests(final Result result)
			{
				return List.copyOf(CrudWorkflowBuilder.this.durableProcessStartRequests.apply(result));
			}
		};
	}

	private CrudWorkflowBuilder(final Supplier<Result> transactionalAction)
	{
		this.transactionalAction = transactionalAction;
	}
}
