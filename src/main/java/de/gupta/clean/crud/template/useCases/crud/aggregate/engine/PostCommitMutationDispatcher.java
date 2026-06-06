package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutation;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.PostCommitMutationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

interface PostCommitMutationDispatcher
{
	static PostCommitMutationDispatcher async()
	{
		Executor executor = command -> Thread.ofVirtual().name("cleancrud-post-commit").start(command);
		return new AsyncPostCommitMutationDispatcher(executor);
	}

	<DomainId, DomainModel> void dispatch(
			PostCommitMutation<DomainId, DomainModel> postCommitMutation,
			PostCommitMutationContext<DomainId, DomainModel> context);

	final class AsyncPostCommitMutationDispatcher implements PostCommitMutationDispatcher
	{
		private static final Logger log = LoggerFactory.getLogger(AsyncPostCommitMutationDispatcher.class);
		private final Executor executor;

		@Override
		public <DomainId, DomainModel> void dispatch(
				final PostCommitMutation<DomainId, DomainModel> postCommitMutation,
				final PostCommitMutationContext<DomainId, DomainModel> context)
		{
			try
			{
				executor.execute(() -> invoke(postCommitMutation, context));
			}
			catch (RuntimeException e)
			{
				log.warn("Failed to schedule post-commit mutation {} for {}", context.kind(), context.domainId(), e);
			}
		}

		private <DomainId, DomainModel> void invoke(
				final PostCommitMutation<DomainId, DomainModel> postCommitMutation,
				final PostCommitMutationContext<DomainId, DomainModel> context)
		{
			try
			{
				postCommitMutation.accept(context);
			}
			catch (RuntimeException e)
			{
				log.warn("Post-commit mutation {} failed for {}", context.kind(), context.domainId(), e);
			}
		}

		private AsyncPostCommitMutationDispatcher(final Executor executor)
		{
			this.executor = executor;
		}
	}
}