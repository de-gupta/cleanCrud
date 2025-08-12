package de.gupta.clean.crud.template.specification;

import de.gupta.clean.crud.template.domain.service.security.DomainSecurityPolicy;

public final class ModelSpecificationFactory
{
	public static <M> ModelSpecification<M> fromDomainSecurityPolicy(final DomainSecurityPolicy<M> policy)
	{
		return policy::isAccessAllowed;
	}
}