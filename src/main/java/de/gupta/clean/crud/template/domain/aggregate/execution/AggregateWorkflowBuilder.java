package de.gupta.clean.crud.template.domain.aggregate.execution;

import de.gupta.clean.crud.template.domain.aggregate.lifecycle.AggregateWorkflow;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class AggregateWorkflowBuilder<Result>
{
	private final Supplier<Result> transactionalAction;
	private Consumer<Result> afterTransaction = _ ->
	{
	};
	private Function<Result, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests = _ -> List.of();
	private boolean readOnly;

	public static <Result> AggregateWorkflowBuilder<Result> writeFlow(final Supplier<Result> transactionalAction)
	{
		return new AggregateWorkflowBuilder<>(transactionalAction);
	}

	public static <Result> AggregateWorkflowBuilder<Result> readOnlyFlow(final Supplier<Result> transactionalAction)
	{
		return new AggregateWorkflowBuilder<>(transactionalAction).readOnly();
	}

	public AggregateWorkflowBuilder<Result> readOnly()
	{
		this.readOnly = true;
		return this;
	}

	public AggregateWorkflowBuilder<Result> afterTransaction(final Consumer<Result> afterTransaction)
	{
		this.afterTransaction = afterTransaction;
		return this;
	}

	public AggregateWorkflowBuilder<Result> startDurableProcesses(
			final Function<Result, Collection<DurableProcessStartRequest<?, ?>>> durableProcessStartRequests)
	{
		this.durableProcessStartRequests = durableProcessStartRequests;
		return this;
	}

	public AggregateWorkflow<Result> build()
	{
		return new AggregateWorkflow<>()
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
				return List.copyOf(AggregateWorkflowBuilder.this.durableProcessStartRequests.apply(result));
			}
		};
	}

	private AggregateWorkflowBuilder(final Supplier<Result> transactionalAction)
	{
		this.transactionalAction = transactionalAction;
	}
}