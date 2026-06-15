package de.gupta.clean.crud.template.domain.aggregate.definition;

import java.util.function.Consumer;

@FunctionalInterface
public interface PostCommitMutation<DomainId, DomainModel>
		extends Consumer<PostCommitMutationContext<DomainId, DomainModel>>
{
	static <DomainId, DomainModel> PostCommitMutation<DomainId, DomainModel> noop()
	{
		return _ ->
		{
		};
	}
}