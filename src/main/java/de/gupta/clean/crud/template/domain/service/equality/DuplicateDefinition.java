package de.gupta.clean.crud.template.domain.service.equality;

@FunctionalInterface
public interface DuplicateDefinition<DomainModel>
{
	boolean areDuplicates(DomainModel left, DomainModel right);
}
