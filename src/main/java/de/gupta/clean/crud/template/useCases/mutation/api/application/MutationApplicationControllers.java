package de.gupta.clean.crud.template.useCases.mutation.api.application;

import de.gupta.clean.crud.template.useCases.mutation.application.service.MutationService;

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

	private static final class DefaultMutationApplicationController<DomainId, DomainModel>
			extends AbstractMutationApplicationController<DomainId, DomainModel>
	{
		private DefaultMutationApplicationController(final MutationService<DomainId, DomainModel> service)
		{
			super(service);
		}
	}
}
