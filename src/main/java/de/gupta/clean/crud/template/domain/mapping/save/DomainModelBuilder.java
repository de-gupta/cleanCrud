package de.gupta.clean.crud.template.domain.mapping.save;

@FunctionalInterface
public interface DomainModelBuilder<DomainModelCreate, DomainModel>
{
	DomainModel toModel(final DomainModelCreate domainModelCreate);
}