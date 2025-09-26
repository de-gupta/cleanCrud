package de.gupta.clean.crud.template.domain.service.security;

@FunctionalInterface
public interface DomainSecurityPolicy<DomainModel>
{
	static <DomainModel> DomainSecurityPolicy<DomainModel> allowing()
	{
		return _ -> true;
	}

	static <DomainModel> DomainSecurityPolicy<DomainModel> denying()
	{
		return _ -> false;
	}

	boolean isAccessAllowed(final DomainModel domainModel);
}