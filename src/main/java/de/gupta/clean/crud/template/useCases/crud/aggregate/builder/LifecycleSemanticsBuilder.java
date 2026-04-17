package de.gupta.clean.crud.template.useCases.crud.aggregate.builder;

import de.gupta.clean.crud.template.useCases.crud.aggregate.lifecycle.LifecycleSemantics;

public final class LifecycleSemanticsBuilder
{
	private boolean cascadeCreate;
	private boolean cascadeUpdate;
	private boolean cascadeDelete;
	private boolean orphanDelete;
	private boolean hydrateOnFetch;

	public static LifecycleSemanticsBuilder lifecycleSemantics()
	{
		return new LifecycleSemanticsBuilder();
	}

	public LifecycleSemanticsBuilder cascadeCreate()
	{
		return cascadeCreate(true);
	}

	public LifecycleSemanticsBuilder cascadeCreate(final boolean enabled)
	{
		this.cascadeCreate = enabled;
		return this;
	}

	public LifecycleSemanticsBuilder cascadeUpdate()
	{
		return cascadeUpdate(true);
	}

	public LifecycleSemanticsBuilder cascadeUpdate(final boolean enabled)
	{
		this.cascadeUpdate = enabled;
		return this;
	}

	public LifecycleSemanticsBuilder cascadeDelete()
	{
		return cascadeDelete(true);
	}

	public LifecycleSemanticsBuilder cascadeDelete(final boolean enabled)
	{
		this.cascadeDelete = enabled;
		return this;
	}

	public LifecycleSemanticsBuilder orphanDelete()
	{
		return orphanDelete(true);
	}

	public LifecycleSemanticsBuilder orphanDelete(final boolean enabled)
	{
		this.orphanDelete = enabled;
		return this;
	}

	public LifecycleSemanticsBuilder hydrateOnFetch()
	{
		return hydrateOnFetch(true);
	}

	public LifecycleSemanticsBuilder hydrateOnFetch(final boolean enabled)
	{
		this.hydrateOnFetch = enabled;
		return this;
	}

	public LifecycleSemantics build()
	{
		return LifecycleSemantics.of(cascadeCreate, cascadeUpdate, cascadeDelete, orphanDelete, hydrateOnFetch);
	}

	private LifecycleSemanticsBuilder()
	{
	}
}
