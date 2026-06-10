package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.api.application.MutationQuarantineApplicationController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/mutation-quarantines")
public class DefaultSpringRestMutationQuarantineController
		extends AbstractSpringRestMutationQuarantineController
		implements SpringRestMutationQuarantineController
{
	public DefaultSpringRestMutationQuarantineController(
			final MutationQuarantineApplicationController applicationController,
			final MutationQuarantineWebMapper webMapper)
	{
		super(applicationController, webMapper);
	}
}
