package de.gupta.clean.crud.template.domain.relationship;

import java.util.Objects;

public record Relationship(
		String propertyName,
		Class<?> satelliteBaseModelClass,
		RelationshipKind relationshipKind,
		Class<?> satelliteApiIdType,
		Class<?> satelliteDomainIdType,
		Class<?> satellitePersistenceIdType,
		ReconciliationStrategy reconciliationStrategy,
		LifecycleSemantics lifecycleSemantics)
{
	public static RelationshipBuilder owned(final String propertyName, final Class<?> satelliteBaseModelClass)
	{
		return new RelationshipBuilder(propertyName, satelliteBaseModelClass, RelationshipKind.OWNED);
	}

	public static RelationshipBuilder referenced(final String propertyName, final Class<?> satelliteBaseModelClass)
	{
		return new RelationshipBuilder(propertyName, satelliteBaseModelClass, RelationshipKind.REFERENCED);
	}

	public Relationship
	{
		Objects.requireNonNull(propertyName, "propertyName");
		Objects.requireNonNull(satelliteBaseModelClass, "satelliteBaseModelClass");
		Objects.requireNonNull(relationshipKind, "relationshipKind");
		Objects.requireNonNull(satelliteApiIdType, "satelliteApiIdType");
		Objects.requireNonNull(satelliteDomainIdType, "satelliteDomainIdType");
		Objects.requireNonNull(satellitePersistenceIdType, "satellitePersistenceIdType");
		Objects.requireNonNull(lifecycleSemantics, "lifecycleSemantics");
	}
}