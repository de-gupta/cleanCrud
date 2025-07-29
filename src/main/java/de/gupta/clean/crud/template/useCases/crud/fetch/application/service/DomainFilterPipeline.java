package de.gupta.clean.crud.template.useCases.crud.fetch.application.service;

import java.util.function.Predicate;
import java.util.function.Supplier;

@FunctionalInterface
public interface DomainFilterPipeline<T>
{
	static <T> DomainFilterPipeline<T> of(Predicate<T> predicate)
	{
		return predicate::test;
	}

	static <T> DomainFilterPipeline<T> allowing()
	{
		return _ -> true;
	}

	default DomainFilterPipeline<T> and(DomainFilterPipeline<T> other)
	{
		return model -> this.allows(model) && other.allows(model);
	}

	boolean allows(T model);

	default DomainFilterPipeline<T> and(Supplier<DomainFilterPipeline<T>> other)
	{
		return model -> this.allows(model) && other.get().allows(model);
	}
}