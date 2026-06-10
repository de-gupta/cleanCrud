package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.application;

import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application.CreationQuarantineService;
import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.domain.policy.CreationQuarantineAccessPolicy;

public final class DefaultCreationQuarantineApplicationController
		extends AbstractCreationQuarantineApplicationController
{
	public DefaultCreationQuarantineApplicationController(
			final CreationQuarantineService service,
			final CreationQuarantineAccessPolicy accessPolicy)
	{
		super(service, accessPolicy);
	}
}
