package de.gupta.clean.crud.template.domain.service.equality;

@FunctionalInterface
public interface DomainEqualityPolicy<DomainModel>
{
	boolean areEqual(DomainModel left, DomainModel right);
}