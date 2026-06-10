package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.api.application;

import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application.MutationQuarantineService;
import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.domain.policy.MutationQuarantineAccessPolicy;

public final class DefaultMutationQuarantineApplicationController
		extends AbstractMutationQuarantineApplicationController
{
	public DefaultMutationQuarantineApplicationController(
			final MutationQuarantineService service,
			final MutationQuarantineAccessPolicy accessPolicy)
	{
		super(service, accessPolicy);
	}
}
