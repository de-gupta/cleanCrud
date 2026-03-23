package de.gupta.clean.crud.template.domain.model.builder;

import java.util.function.Supplier;

public final class BuilderFactories
{
	public static <Model, Builder extends ModelBuilder<? extends Model>> ModelBuilderFactory<Model, Builder> of(
			final Supplier<Builder> supplier)
	{
		return supplier::get;
	}

	private BuilderFactories()
	{
	}
}
