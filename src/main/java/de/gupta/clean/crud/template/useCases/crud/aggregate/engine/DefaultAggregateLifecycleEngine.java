package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;

public final class DefaultAggregateLifecycleEngine implements AggregateLifecycleEngine
{
	private final PersistenceTransactionRunner transactionRunner;
	private final PostCommitMutationDispatcher postCommitMutationDispatcher;

	public static DefaultAggregateLifecycleEngine withTransactionRunner(
			final PersistenceTransactionRunner transactionRunner)
	{
		return new DefaultAggregateLifecycleEngine(transactionRunner, PostCommitMutationDispatcher.async());
	}

	static DefaultAggregateLifecycleEngine withTransactionRunnerAndDispatcher(
			final PersistenceTransactionRunner transactionRunner,
			final PostCommitMutationDispatcher postCommitMutationDispatcher)
	{
		return new DefaultAggregateLifecycleEngine(transactionRunner, postCommitMutationDispatcher);
	}

	@Override
	public <Result> Result execute(final CrudWorkflow<Result> workflow)
	{
		var result = transactionRunner.inTransaction(workflow::inTransaction);
		postCommitMutationDispatcher.dispatch(() -> workflow.afterTransaction(result));
		return result;
	}

	private DefaultAggregateLifecycleEngine(
			final PersistenceTransactionRunner transactionRunner,
			final PostCommitMutationDispatcher postCommitMutationDispatcher)
	{
		this.transactionRunner = transactionRunner;
		this.postCommitMutationDispatcher = postCommitMutationDispatcher;
	}
}