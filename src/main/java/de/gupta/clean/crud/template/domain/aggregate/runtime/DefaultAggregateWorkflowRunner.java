package de.gupta.clean.crud.template.domain.aggregate.runtime;

import de.gupta.clean.crud.template.domain.aggregate.workflow.AggregateWorkflow;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.process.application.execution.DurableProcessExecutionNudge;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStarter;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;

import java.util.ArrayList;
import java.util.List;

public final class DefaultAggregateWorkflowRunner implements AggregateWorkflowRunner
{
	private final PersistenceTransactionRunner transactionRunner;
	private final PostCommitMutationDispatcher postCommitMutationDispatcher;
	private final DurableProcessStarter durableProcessStarter;
	private final DurableProcessExecutionNudge durableProcessExecutionNudge;

	public static AggregateWorkflowRunner withTransactionRunner(final PersistenceTransactionRunner transactionRunner)
	{
		return new DefaultAggregateWorkflowRunner(transactionRunner, PostCommitMutationDispatcher.async(),
				unsupportedDurableProcessStarter(), DurableProcessExecutionNudge.noop());
	}

	private static DurableProcessStarter unsupportedDurableProcessStarter()
	{
		return _ ->
		{
			throw new UnsupportedOperationException("Durable process starter not configured");
		};
	}

	public static AggregateWorkflowRunner withTransactionRunnerAndDurableProcessStarter(
			final PersistenceTransactionRunner transactionRunner, final DurableProcessStarter durableProcessStarter)
	{
		return withTransactionRunnerAndDurableProcessStarterAndExecutionNudge(transactionRunner, durableProcessStarter,
				DurableProcessExecutionNudge.noop());
	}

	public static AggregateWorkflowRunner withTransactionRunnerAndDurableProcessStarterAndExecutionNudge(
			final PersistenceTransactionRunner transactionRunner, final DurableProcessStarter durableProcessStarter,
			final DurableProcessExecutionNudge durableProcessExecutionNudge)
	{
		return new DefaultAggregateWorkflowRunner(transactionRunner, PostCommitMutationDispatcher.async(),
				durableProcessStarter, durableProcessExecutionNudge);
	}

	static AggregateWorkflowRunner withTransactionRunnerAndDispatcher(
			final PersistenceTransactionRunner transactionRunner,
			final PostCommitMutationDispatcher postCommitMutationDispatcher)
	{
		return new DefaultAggregateWorkflowRunner(transactionRunner, postCommitMutationDispatcher,
				unsupportedDurableProcessStarter(), DurableProcessExecutionNudge.noop());
	}

	static AggregateWorkflowRunner withTransactionRunnerDispatcherStarterAndExecutionNudge(
			final PersistenceTransactionRunner transactionRunner,
			final PostCommitMutationDispatcher postCommitMutationDispatcher,
			final DurableProcessStarter durableProcessStarter,
			final DurableProcessExecutionNudge durableProcessExecutionNudge)
	{
		return new DefaultAggregateWorkflowRunner(transactionRunner, postCommitMutationDispatcher,
				durableProcessStarter,
				durableProcessExecutionNudge);
	}

	@Override
	public <Result> Result run(final AggregateWorkflow<Result> workflow)
	{
		var startedTaskIds = new ArrayList<DurableProcessTaskId>();
		var result = transactionRunner.inTransaction(() ->
		{
			var transactionalResult = workflow.inTransaction();
			workflow.durableProcessStartRequests(transactionalResult)
			        .forEach(startRequest -> startedTaskIds.add(durableProcessStarter.start(startRequest)));
			return transactionalResult;
		});
		postCommitMutationDispatcher.dispatch(() ->
		{
			workflow.afterTransaction(result);
			durableProcessExecutionNudge.afterCommit(List.copyOf(startedTaskIds));
		});
		return result;
	}

	private DefaultAggregateWorkflowRunner(final PersistenceTransactionRunner transactionRunner,
	                                       final PostCommitMutationDispatcher postCommitMutationDispatcher,
	                                       final DurableProcessStarter durableProcessStarter,
	                                       final DurableProcessExecutionNudge durableProcessExecutionNudge)
	{
		this.transactionRunner = transactionRunner;
		this.postCommitMutationDispatcher = postCommitMutationDispatcher;
		this.durableProcessStarter = durableProcessStarter;
		this.durableProcessExecutionNudge = durableProcessExecutionNudge;
	}
}