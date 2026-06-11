package de.gupta.clean.crud.template.useCases.operation.quarantine.api.application;

import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.QuarantineService;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.PayloadReplayInputs;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.policy.QuarantineAccessPolicy;

public final class DefaultQuarantineApplicationController<P extends PayloadReplayInputs>
		extends AbstractQuarantineApplicationController<P>
{
	DefaultQuarantineApplicationController(
			final QuarantineService<P> service,
			final QuarantineAccessPolicy<P> accessPolicy)
	{
		super(service, accessPolicy);
	}
}
