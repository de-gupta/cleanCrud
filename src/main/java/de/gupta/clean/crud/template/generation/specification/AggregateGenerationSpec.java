package de.gupta.clean.crud.template.generation.specification;

import de.gupta.clean.crud.template.domain.relationship.Relationship;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record AggregateGenerationSpec(
		Class<?> baseModelClass,
		Collection<Relationship> relationships,
		Class<?> rootApiIdType,
		Class<?> rootDomainIdType,
		Class<?> rootPersistenceIdType,
		AggregateGenerationOperationFlags postCommitHooks,
		AggregateGenerationOperationFlags subprocesses)
{
	public AggregateGenerationSpec
	{
		Objects.requireNonNull(baseModelClass, "baseModelClass");
		relationships = List.copyOf(Objects.requireNonNull(relationships, "relationships"));
		postCommitHooks = postCommitHooks == null ? AggregateGenerationOperationFlags.disabled() : postCommitHooks;
		subprocesses = subprocesses == null ? AggregateGenerationOperationFlags.disabled() : subprocesses;
	}
}
