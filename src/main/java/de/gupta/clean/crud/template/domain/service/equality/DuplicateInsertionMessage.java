package de.gupta.clean.crud.template.domain.service.equality;

@FunctionalInterface
public interface DuplicateInsertionMessage<DomainModel>
{
	String messageIfModelAlreadyExists(final DomainModel domainModel);
}