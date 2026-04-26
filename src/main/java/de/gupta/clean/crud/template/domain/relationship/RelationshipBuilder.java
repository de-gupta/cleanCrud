package de.gupta.clean.crud.template.domain.relationship;

import de.gupta.aletheia.functional.Unfolding;

public final class RelationshipBuilder
{
	private final String propertyName;
	private final Class<?> satelliteBaseModelClass;
	private final RelationshipKind relationshipKind;
	private Class<?> satelliteApiIdType;
	private Class<?> satelliteDomainIdType;
	private Class<?> satellitePersistenceIdType;
	private ReconciliationStrategy reconciliationStrategy;
	private LifecycleSemantics lifecycleSemantics;
	private Boolean cascadeCreate;
	private Boolean cascadeUpdate;
	private Boolean cascadeDelete;
	private Boolean orphanDelete;
	private Boolean hydrateOnFetch;

	public RelationshipBuilder satelliteApiIdType(final Class<?> satelliteApiIdType)
	{
		this.satelliteApiIdType = satelliteApiIdType;
		return this;
	}

	public RelationshipBuilder satelliteDomainIdType(final Class<?> satelliteDomainIdType)
	{
		this.satelliteDomainIdType = satelliteDomainIdType;
		return this;
	}

	public RelationshipBuilder satellitePersistenceIdType(final Class<?> satellitePersistenceIdType)
	{
		this.satellitePersistenceIdType = satellitePersistenceIdType;
		return this;
	}

	public RelationshipBuilder reconciliationStrategy(final ReconciliationStrategy reconciliationStrategy)
	{
		this.reconciliationStrategy = reconciliationStrategy;
		return this;
	}

	public RelationshipBuilder lifecycleSemantics(final LifecycleSemantics lifecycleSemantics)
	{
		this.lifecycleSemantics = lifecycleSemantics;
		return this;
	}

	public RelationshipBuilder lifecycleSemantics(
			final boolean cascadeCreate,
			final boolean cascadeUpdate,
			final boolean cascadeDelete,
			final boolean orphanDelete,
			final boolean hydrateOnFetch)
	{
		this.cascadeCreate = cascadeCreate;
		this.cascadeUpdate = cascadeUpdate;
		this.cascadeDelete = cascadeDelete;
		this.orphanDelete = orphanDelete;
		this.hydrateOnFetch = hydrateOnFetch;
		return this;
	}

	public RelationshipBuilder cascadeCreate(final boolean cascadeCreate)
	{
		this.cascadeCreate = cascadeCreate;
		return this;
	}

	public RelationshipBuilder cascadeUpdate(final boolean cascadeUpdate)
	{
		this.cascadeUpdate = cascadeUpdate;
		return this;
	}

	public RelationshipBuilder cascadeDelete(final boolean cascadeDelete)
	{
		this.cascadeDelete = cascadeDelete;
		return this;
	}

	public RelationshipBuilder orphanDelete(final boolean orphanDelete)
	{
		this.orphanDelete = orphanDelete;
		return this;
	}

	public RelationshipBuilder hydrateOnFetch(final boolean hydrateOnFetch)
	{
		this.hydrateOnFetch = hydrateOnFetch;
		return this;
	}

	public Relationship build()
	{
		var effectiveLifecycleSemantics = lifecycleSemantics == null
				? defaultsFor(relationshipKind)
				: lifecycleSemantics;
		return new Relationship(
				propertyName,
				satelliteBaseModelClass,
				relationshipKind,
				satelliteApiIdType,
				satelliteDomainIdType,
				satellitePersistenceIdType,
				reconciliationStrategy,
				LifecycleSemantics.of(
						Unfolding.beckon(cascadeCreate)
						         .infuse(effectiveLifecycleSemantics.cascadeCreate()),
						Unfolding.beckon(cascadeUpdate)
						         .infuse(effectiveLifecycleSemantics.cascadeUpdate()),
						Unfolding.beckon(cascadeDelete)
						         .infuse(effectiveLifecycleSemantics.cascadeDelete()),
						Unfolding.beckon(orphanDelete)
						         .infuse(effectiveLifecycleSemantics.orphanDelete()),
						Unfolding.beckon(hydrateOnFetch)
						         .infuse(effectiveLifecycleSemantics.hydrateOnFetch()))
		);
	}

	private static LifecycleSemantics defaultsFor(final RelationshipKind relationshipKind)
	{
		return relationshipKind == RelationshipKind.OWNED
				? LifecycleSemantics.of(true, true, false, false, true)
				: LifecycleSemantics.of(false, true, false, false, true);
	}

	RelationshipBuilder(
			final String propertyName,
			final Class<?> satelliteBaseModelClass,
			final RelationshipKind relationshipKind)
	{
		this.propertyName = propertyName;
		this.satelliteBaseModelClass = satelliteBaseModelClass;
		this.relationshipKind = relationshipKind;
	}
}
