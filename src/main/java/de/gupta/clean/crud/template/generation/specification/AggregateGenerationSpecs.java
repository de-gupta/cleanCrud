package de.gupta.clean.crud.template.generation.specification;

import de.gupta.clean.crud.template.domain.relationship.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AggregateGenerationSpecs
{
	public static AggregateGenerationSpecBuilder aggregate(final Class<?> baseModelClass)
	{
		return new AggregateGenerationSpecBuilder(baseModelClass);
	}

	private AggregateGenerationSpecs()
	{
	}

	public static final class AggregateGenerationSpecBuilder
	{
		private final Class<?> baseModelClass;
		private final List<Relationship> relationships = new ArrayList<>();
		private Class<?> rootApiIdType;
		private Class<?> rootDomainIdType;
		private Class<?> rootPersistenceIdType;
		private boolean postCommitSave;
		private boolean postCommitUpdate;
		private boolean postCommitDelete;
		private boolean subprocessSave;
		private boolean subprocessUpdate;
		private boolean subprocessDelete;

		public AggregateGenerationSpecBuilder rootApiIdType(final Class<?> rootApiIdType)
		{
			this.rootApiIdType = Objects.requireNonNull(rootApiIdType, "rootApiIdType");
			return this;
		}

		public AggregateGenerationSpecBuilder rootDomainIdType(final Class<?> rootDomainIdType)
		{
			this.rootDomainIdType = Objects.requireNonNull(rootDomainIdType, "rootDomainIdType");
			return this;
		}

		public AggregateGenerationSpecBuilder rootPersistenceIdType(final Class<?> rootPersistenceIdType)
		{
			this.rootPersistenceIdType = Objects.requireNonNull(rootPersistenceIdType, "rootPersistenceIdType");
			return this;
		}

		public AggregateGenerationSpecBuilder relationship(final Relationship relationship)
		{
			relationships.add(Objects.requireNonNull(relationship, "relationship"));
			return this;
		}

		public AggregateGenerationSpecBuilder postCommitOnSave()
		{
			postCommitSave = true;
			return this;
		}

		public AggregateGenerationSpecBuilder postCommitOnUpdate()
		{
			postCommitUpdate = true;
			return this;
		}

		public AggregateGenerationSpecBuilder postCommitOnDelete()
		{
			postCommitDelete = true;
			return this;
		}

		public AggregateGenerationSpecBuilder subprocessOnSave()
		{
			subprocessSave = true;
			return this;
		}

		public AggregateGenerationSpecBuilder subprocessOnUpdate()
		{
			subprocessUpdate = true;
			return this;
		}

		public AggregateGenerationSpecBuilder subprocessOnDelete()
		{
			subprocessDelete = true;
			return this;
		}

		public AggregateGenerationSpec build()
		{
			return new AggregateGenerationSpec(
					baseModelClass,
					List.copyOf(relationships),
					rootApiIdType,
					rootDomainIdType,
					rootPersistenceIdType,
					new AggregateGenerationOperationFlags(postCommitSave, postCommitUpdate, postCommitDelete),
					new AggregateGenerationOperationFlags(subprocessSave, subprocessUpdate, subprocessDelete));
		}

		private AggregateGenerationSpecBuilder(final Class<?> baseModelClass)
		{
			this.baseModelClass = Objects.requireNonNull(baseModelClass, "baseModelClass");
		}
	}
}
