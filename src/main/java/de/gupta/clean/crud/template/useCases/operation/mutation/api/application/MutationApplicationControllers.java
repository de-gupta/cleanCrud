package de.gupta.clean.crud.template.useCases.operation.mutation.api.application;

import de.gupta.clean.crud.template.useCases.operation.mutation.application.service.MutationService;

public final class MutationApplicationControllers
{
	public static <DomainId, DomainModel> MutationApplicationController<DomainId, DomainModel> controller(
			final MutationService<DomainId, DomainModel> service)
	{
		return new DefaultMutationApplicationController<>(service);
	}

	private MutationApplicationControllers()
	{
	}
}
