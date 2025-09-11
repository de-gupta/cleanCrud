package de.gupta.clean.crud.template.useCases.query.specification.domain.model;

public final class ActiveFilterSpecification implements LeafFilterSpecification
{
	private static final ActiveFilterSpecification INSTANCE = new ActiveFilterSpecification();

	public static ActiveFilterSpecification instance()
	{
		return INSTANCE;
	}
}