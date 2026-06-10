package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.application;

import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.CreationQuarantineService;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.policy.CreationQuarantineAccessPolicy;

public final class CreationQuarantineApplicationControllers
{
	public static CreationQuarantineApplicationController controller(
			final CreationQuarantineService service,
			final CreationQuarantineAccessPolicy accessPolicy)
	{
		return new DefaultCreationQuarantineApplicationController(service, accessPolicy);
	}

	private CreationQuarantineApplicationControllers()
	{
	}
}
