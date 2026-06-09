package de.gupta.clean.crud.template.useCases.mutation.quarantine.api.application;

import de.gupta.clean.crud.template.useCases.mutation.quarantine.application.MutationQuarantineService;
import de.gupta.clean.crud.template.useCases.mutation.quarantine.domain.policy.MutationQuarantineAccessPolicy;

public final class MutationQuarantineApplicationControllers
{
	public static MutationQuarantineApplicationController controller(
			final MutationQuarantineService service,
			final MutationQuarantineAccessPolicy accessPolicy)
	{
		return new DefaultMutationQuarantineApplicationController(service, accessPolicy);
	}

	private MutationQuarantineApplicationControllers()
	{
	}
}
