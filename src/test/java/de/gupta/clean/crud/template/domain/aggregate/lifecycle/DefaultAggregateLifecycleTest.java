package de.gupta.clean.crud.template.domain.aggregate.lifecycle;

import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationContext;
import de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutationKind;
import de.gupta.clean.crud.template.infrastructure.persistence.transaction.PersistenceTransactionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultAggregateLifecycle")
final class DefaultAggregateLifecycleTest
{
	private static final class TestTransactionRunner implements PersistenceTransactionRunner
	{
		private int transactionCount;

		@Override
		public <T> T inTransaction(final Supplier<T> action)
		{
			transactionCount++;
			return action.get();
		}

		int transactionCount()
		{
			return transactionCount;
		}
	}

	private static class RecordingDispatcher implements PostCommitMutationDispatcher
	{
		@Override
		public <DomainId, DomainModel> void dispatch(
				final de.gupta.clean.crud.template.domain.aggregate.definition.PostCommitMutation<DomainId, DomainModel> postCommitMutation,
				final PostCommitMutationContext<DomainId, DomainModel> context)
		{
			postCommitMutation.accept(context);
		}
	}

	private static final class SwallowingRecordingDispatcher extends RecordingDispatcher
	{
		@Override
		public void dispatch(final Runnable action)
		{
			try
			{
				action.run();
			}
			catch (RuntimeException ignored)
			{
			}
		}
	}

	@Nested
	@DisplayName("when executing a workflow")
	final class WhenExecutingAWorkflow
	{
		@Test
		@DisplayName("it runs the workflow inside a transaction")
		void itRunsTheWorkflowInsideATransaction()
		{
			var transactionRunner = new TestTransactionRunner();
			var lifecycle = DefaultAggregateLifecycle.withTransactionRunner(transactionRunner);

			var result = lifecycle.execute((AggregateWorkflow<Object>) () -> "done");

			assertThat(result).isEqualTo("done");
			assertThat(transactionRunner.transactionCount()).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("when running after-transaction hooks")
	final class WhenRunningAfterTransactionHooks
	{
		@Test
		@DisplayName("it dispatches the committed context after commit")
		void itDispatchesTheCommittedContextAfterCommit()
		{
			var dispatcher = new RecordingDispatcher();
			var transactionRunner = new TestTransactionRunner();
			var lifecycle = DefaultAggregateLifecycle.withTransactionRunnerAndDispatcher(transactionRunner, dispatcher);
			var contexts = new ArrayList<PostCommitMutationContext<String, String>>();

			lifecycle.execute(new AggregateWorkflow<PostCommitMutationContext<String, String>>()
			{
				@Override
				public PostCommitMutationContext<String, String> inTransaction()
				{
					return new PostCommitMutationContext<>(
							PostCommitMutationKind.CREATE,
							"id-1",
							Optional.of("after"),
							Optional.empty());
				}

				@Override
				public void afterTransaction(final PostCommitMutationContext<String, String> result)
				{
					dispatcher.dispatch(contexts::add, result);
				}
			});

			assertThat(contexts).containsExactly(new PostCommitMutationContext<>(
					PostCommitMutationKind.CREATE,
					"id-1",
					Optional.of("after"),
					Optional.empty()));
		}

		@Test
		@DisplayName("it does not dispatch when the transaction fails")
		void itDoesNotDispatchWhenTheTransactionFails()
		{
			var dispatcher = new RecordingDispatcher();
			var transactionRunner = new TestTransactionRunner();
			var lifecycle = DefaultAggregateLifecycle.withTransactionRunnerAndDispatcher(transactionRunner, dispatcher);
			var contexts = new ArrayList<PostCommitMutationContext<String, String>>();

			assertThatThrownBy(() -> lifecycle.execute(new AggregateWorkflow<String>()
			{
				@Override
				public String inTransaction()
				{
					throw new IllegalStateException("boom");
				}

				@Override
				public void afterTransaction(final String result)
				{
					dispatcher.dispatch(contexts::add, new PostCommitMutationContext<>(
							PostCommitMutationKind.CREATE,
							"id-1",
							Optional.of(result),
							Optional.empty()));
				}
			})).isInstanceOf(IllegalStateException.class)
			   .hasMessage("boom");

			assertThat(contexts).isEmpty();
		}

		@Test
		@DisplayName("it keeps the committed result when the dispatcher swallows hook failures")
		void itKeepsTheCommittedResultWhenTheDispatcherSwallowsHookFailures()
		{
			var dispatcher = new SwallowingRecordingDispatcher();
			var transactionRunner = new TestTransactionRunner();
			var lifecycle = DefaultAggregateLifecycle.withTransactionRunnerAndDispatcher(transactionRunner, dispatcher);
			var committedValues = new ArrayList<String>();

			var result = lifecycle.execute(new AggregateWorkflow<String>()
			{
				@Override
				public String inTransaction()
				{
					committedValues.add("saved");
					return "saved";
				}

				@Override
				public void afterTransaction(final String result)
				{
					throw new RuntimeException("hook failed");
				}
			});

			assertThat(result).isEqualTo("saved");
			assertThat(committedValues).containsExactly("saved");
			assertThat(transactionRunner.transactionCount()).isEqualTo(1);
		}
	}
}