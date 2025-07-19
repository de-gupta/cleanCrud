package de.gupta.clean.crud.template.domain.model.identified;

public record IdentifiedModel<ID, Model>(ID id, Model model)
{
	public static <I, M> IdentifiedModel<I, M> of(I id, M model)
	{
		return new IdentifiedModel<>(id, model);
	}
}