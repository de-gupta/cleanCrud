package de.gupta.clean.crud.template.specification;

public interface ModelSpecification<Model>
{
	boolean satisfies(final Model model);
}