package de.gupta.clean.crud.template.domain.model.builder;

import de.gupta.clean.crud.template.domain.model.validation.Validatable;

public abstract class AbstractModelBuilder<Model extends Validatable> implements ModelBuilder<Model>
{
	@Override
	public final Model build()
	{
		setOptionalFieldsToDefaultValues();
		Model model = doBuild();
		model.validate();
		return model;
	}

	protected void setOptionalFieldsToDefaultValues()
	{
	}

	protected abstract Model doBuild();
}