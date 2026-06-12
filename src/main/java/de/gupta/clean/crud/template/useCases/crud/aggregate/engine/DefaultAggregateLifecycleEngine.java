package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.quarantine.application.recording.CreationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.quarantine.application.recording.MutationQuarantineRecorder;
import de.gupta.clean.crud.template.useCases.process.application.execution.DurableProcessExecutionNudge;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStarter;

import java.util.ArrayList;
import java.util.List;

public final class DefaultAggregateLifecycleEngine implements AggregateLifecycleEngine
{
	private final PersistenceTransactionRunner transactionRunner;
	private final PostCommitMutationDispatcher postCommitMutationDispatcher;
	private final DurableProcessStarter durableProcessStarter;
	private final DurableProcessExecutionNudge durableProcessExecutionNudge;
	private final MutationQuarantineRecorder mutationQuarantineRecorder;
	private final CreationQuarantineRecorder creationQuarantineRecorder;

	public static DefaultAggregateLifecycleEngine withTransactionRunner(
			final PersistenceTransactionRunner transactionRunner)
	{
		return new DefaultAggregateLifecycleEngine(
				transactionRunner,
				PostCommitMutationDispatcher.async(),
				unsupportedDurableProcessStarter(),
				DurableProcessExecutionNudge.noop(),
				MutationQuarantineRecorder.noop(),
				CreationQuarantineRecorder.noop());
	}

	public static DefaultAggregateLifecycleEngine withTransactionRunnerAndMutationQuarantineRecorder(
			final PersistenceTransactionRunner transactionRunner,
			final MutationQuarantineRecorder mutationQuarantineRecorder)
	{
		return new DefaultAggregateLifecycleEngine(
				transactionRunner,
				PostCommitMutationDispatcher.async(),
				unsupportedDurableProcessStarter(),
				DurableProcessExecutionNudge.noop(),
				mutationQuarantineRecorder,
				CreationQuarantineRecorder.noop());
	}

	public static DefaultAggregateLifecycleEngine withTransactionRunnerAndQuarantineRecorders(
			final PersistenceTransactionRunner transactionRunner,
			final MutationQuarantineRecorder mutationQuarantineRecorder,
			final CreationQuarantineRecorder creationQuarantineRecorder)
	{
		return new DefaultAggregateLifecycleEngine(
				transactionRunner,
				PostCommitMutationDispatcher.async(),
				unsupportedDurableProcessStarter(),
				DurableProcessExecutionNudge.noop(),
				mutationQuarantineRecorder,
				creationQuarantineRecorder);
	}

	public static DefaultAggregateLifecycleEngine withTransactionRunnerAndDurableProcessStarter(
			final PersistenceTransactionRunner transactionRunner,
			final DurableProcessStarter durableProcessStarter)
	{
		return withTransactionRunnerAndDurableProcessStarterAndExecutionNudge(
				transactionRunner,
				durableProcessStarter,
				DurableProcessExecutionNudge.noop());
	}

	public static DefaultAggregateLifecycleEngine withTransactionRunnerAndDurableProcessStarterAndExecutionNudge(
			final PersistenceTransactionRunner transactionRunner,
			final DurableProcessStarter durableProcessStarter,
			final DurableProcessExecutionNudge durableProcessExecutionNudge)
	{
		return new DefaultAggregateLifecycleEngine(
				transactionRunner,
				PostCommitMutationDispatcher.async(),
				durableProcessStarter,
				durableProcessExecutionNudge,
				MutationQuarantineRecorder.noop(),
				CreationQuarantineRecorder.noop());
	}

	public static DefaultAggregateLifecycleEngine withTransactionRunnerAndDurableProcessStarterExecutionNudgeAndMutationQuarantineRecorder(
			final PersistenceTransactionRunner transactionRunner,
			final DurableProcessStarter durableProcessStarter,
			final DurableProcessExecutionNudge durableProcessExecutionNudge,
			final MutationQuarantineRecorder mutationQuarantineRecorder)
	{
		return new DefaultAggregateLifecycleEngine(
				transactionRunner,
				PostCommitMutationDispatcher.async(),
				durableProcessStarter,
				durableProcessExecutionNudge,
				mutationQuarantineRecorder,
				CreationQuarantineRecorder.noop());
	}

	public static DefaultAggregateLifecycleEngine withTransactionRunnerAndDurableProcessStarterExecutionNudgeAndQuarantineRecorders(
			final PersistenceTransactionRunner transactionRunner,
			final DurableProcessStarter durableProcessStarter,
			final DurableProcessExecutionNudge durableProcessExecutionNudge,
			final MutationQuarantineRecorder mutationQuarantineRecorder,
			final CreationQuarantineRecorder creationQuarantineRecorder)
	{
		return new DefaultAggregateLifecycleEngine(
				transactionRunner,
				PostCommitMutationDispatcher.async(),
				durableProcessStarter,
				durableProcessExecutionNudge,
				mutationQuarantineRecorder,
				creationQuarantineRecorder);
	}

	static DefaultAggregateLifecycleEngine withTransactionRunnerAndDispatcher(
			final PersistenceTransactionRunner transactionRunner,
			final PostCommitMutationDispatcher postCommitMutationDispatcher)
	{
		return new DefaultAggregateLifecycleEngine(
				transactionRunner,
				postCommitMutationDispatcher,
				unsupportedDurableProcessStarter(),
				DurableProcessExecutionNudge.noop(),
				MutationQuarantineRecorder.noop(),
				CreationQuarantineRecorder.noop());
	}

	static DefaultAggregateLifecycleEngine withTransactionRunnerDispatcherAndStarter(
			final PersistenceTransactionRunner transactionRunner,
			final PostCommitMutationDispatcher postCommitMutationDispatcher,
			final DurableProcessStarter durableProcessStarter)
	{
		return withTransactionRunnerDispatcherStarterAndExecutionNudge(
				transactionRunner,
				postCommitMutationDispatcher,
				durableProcessStarter,
				DurableProcessExecutionNudge.noop());
	}

	static DefaultAggregateLifecycleEngine withTransactionRunnerDispatcherStarterAndExecutionNudge(
			final PersistenceTransactionRunner transactionRunner,
			final PostCommitMutationDispatcher postCommitMutationDispatcher,
			final DurableProcessStarter durableProcessStarter,
			final DurableProcessExecutionNudge durableProcessExecutionNudge)
	{
		return new DefaultAggregateLifecycleEngine(transactionRunner, postCommitMutationDispatcher,
				durableProcessStarter,
				durableProcessExecutionNudge,
				MutationQuarantineRecorder.noop(),
				CreationQuarantineRecorder.noop());
	}

	@Override
	public <Result> Result execute(final CrudWorkflow<Result> workflow)
	{
		var startedTaskIds =
				new ArrayList<de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId>();
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

	@Override
	public MutationQuarantineRecorder mutationQuarantineRecorder()
	{
		return mutationQuarantineRecorder;
	}

	@Override
	public CreationQuarantineRecorder creationQuarantineRecorder()
	{
		return creationQuarantineRecorder;
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
			final DurableProcessStarter durableProcessStarter,
			final DurableProcessExecutionNudge durableProcessExecutionNudge,
			final MutationQuarantineRecorder mutationQuarantineRecorder,
			final CreationQuarantineRecorder creationQuarantineRecorder)
	{
		this.transactionRunner = transactionRunner;
		this.postCommitMutationDispatcher = postCommitMutationDispatcher;
		this.durableProcessStarter = durableProcessStarter;
		this.durableProcessExecutionNudge = durableProcessExecutionNudge;
		this.mutationQuarantineRecorder = mutationQuarantineRecorder;
		this.creationQuarantineRecorder = creationQuarantineRecorder;
	}
}