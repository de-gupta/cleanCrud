package de.gupta.clean.crud.template.useCases.mutation.api.application;

import de.gupta.clean.crud.template.useCases.mutation.application.service.MutationService;

public final class DefaultMutationApplicationController<DomainId, DomainModel>
		extends AbstractMutationApplicationController<DomainId, DomainModel>
{
	public DefaultMutationApplicationController(final MutationService<DomainId, DomainModel> service)
	{
		super(service);
	}
}
