package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStarter;

public final class DefaultAggregateLifecycleEngine implements AggregateLifecycleEngine
{
	private final PersistenceTransactionRunner transactionRunner;
	private final PostCommitMutationDispatcher postCommitMutationDispatcher;
	private final DurableProcessStarter durableProcessStarter;

	public static DefaultAggregateLifecycleEngine withTransactionRunner(
			final PersistenceTransactionRunner transactionRunner)
	{
		return new DefaultAggregateLifecycleEngine(
				transactionRunner,
				PostCommitMutationDispatcher.async(),
				unsupportedDurableProcessStarter());
	}

	public static DefaultAggregateLifecycleEngine withTransactionRunnerAndDurableProcessStarter(
			final PersistenceTransactionRunner transactionRunner,
			final DurableProcessStarter durableProcessStarter)
	{
		return new DefaultAggregateLifecycleEngine(
				transactionRunner,
				PostCommitMutationDispatcher.async(),
				durableProcessStarter);
	}

	static DefaultAggregateLifecycleEngine withTransactionRunnerAndDispatcher(
			final PersistenceTransactionRunner transactionRunner,
			final PostCommitMutationDispatcher postCommitMutationDispatcher)
	{
		return new DefaultAggregateLifecycleEngine(
				transactionRunner,
				postCommitMutationDispatcher,
				unsupportedDurableProcessStarter());
	}

	static DefaultAggregateLifecycleEngine withTransactionRunnerDispatcherAndStarter(
			final PersistenceTransactionRunner transactionRunner,
			final PostCommitMutationDispatcher postCommitMutationDispatcher,
			final DurableProcessStarter durableProcessStarter)
	{
		return new DefaultAggregateLifecycleEngine(transactionRunner, postCommitMutationDispatcher,
				durableProcessStarter);
	}

	@Override
	public <Result> Result execute(final CrudWorkflow<Result> workflow)
	{
		var result = transactionRunner.inTransaction(() ->
		{
			var transactionalResult = workflow.inTransaction();
			workflow.durableProcessStartRequests(transactionalResult).forEach(durableProcessStarter::start);
			return transactionalResult;
		});
		postCommitMutationDispatcher.dispatch(() -> workflow.afterTransaction(result));
		return result;
	}

	private static DurableProcessStarter unsupportedDurableProcessStarter()
	{
		return _ ->
		{
			throw new UnsupportedOperationException("Durable process starter not configured");
		};
	}

	private DefaultAggregateLifecycleEngine(
			final PersistenceTransactionRunner transactionRunner,
			final PostCommitMutationDispatcher postCommitMutationDispatcher,
			final DurableProcessStarter durableProcessStarter)
	{
		this.transactionRunner = transactionRunner;
		this.postCommitMutationDispatcher = postCommitMutationDispatcher;
		this.durableProcessStarter = durableProcessStarter;
	}
}