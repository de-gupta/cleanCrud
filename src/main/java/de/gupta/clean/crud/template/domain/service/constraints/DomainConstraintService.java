package de.gupta.clean.crud.template.domain.service.constraints;

public interface DomainConstraintService<DomainModel>
{
	ConstraintResult validateForInsertion(final DomainModel model);

	ConstraintResult validateForUpdate(final DomainModel originalModel, final DomainModel updatedModel);
}