package de.gupta.clean.crud.template.domain.service.security;

@FunctionalInterface
public interface DomainSecurityPolicy<DomainModel>
{
	boolean isAccessAllowed(final DomainModel domainModel);
}