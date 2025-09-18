package de.gupta.clean.crud.template.domain.service.constraints;

public interface ExistingModelsConstraintService<DomainModel>
{
	ConstraintResult mayThisResourceBeAdded(final DomainModel model);

	ConstraintResult mayThisResourceBeChangedTo(final DomainModel originalModel, final DomainModel newModel);
}