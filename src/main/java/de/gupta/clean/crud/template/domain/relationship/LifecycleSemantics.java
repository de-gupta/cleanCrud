package de.gupta.clean.crud.template.domain.relationship;

public record LifecycleSemantics(
		boolean cascadeCreate,
		boolean cascadeUpdate,
		boolean cascadeDelete,
		boolean orphanDelete,
		boolean hydrateOnFetch)
{
	public static LifecycleSemantics none()
	{
		return new LifecycleSemantics(false, false, false, false, false);
	}

	public static LifecycleSemantics of(
			final boolean cascadeCreate,
			final boolean cascadeUpdate,
			final boolean cascadeDelete,
			final boolean orphanDelete,
			final boolean hydrateOnFetch)
	{
		return new LifecycleSemantics(
				cascadeCreate,
				cascadeUpdate,
				cascadeDelete,
				orphanDelete,
				hydrateOnFetch);
	}
}