package de.gupta.clean.crud.template.useCases.operation.quarantine.api.application;

import de.gupta.clean.crud.template.useCases.operation.quarantine.application.service.QuarantineService;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.policy.QuarantineAccessPolicy;

public final class QuarantineApplicationControllers
{
	public static <P> QuarantineApplicationController<P> controller(
			final QuarantineService<P> service,
			final QuarantineAccessPolicy<P> accessPolicy)
	{
		return new DefaultQuarantineApplicationController<>(service, accessPolicy);
	}

	private QuarantineApplicationControllers()
	{
	}
}
