package de.gupta.clean.crud.template.domain.service.constraints;

@FunctionalInterface
public interface DomainConstraintService<DomainModel>
{
	ConstraintResult mayThisResourceBeAdded(final DomainModel model);
}