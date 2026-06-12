package de.gupta.clean.crud.template.useCases.operationOLD.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.api.application.QuarantineApplicationController;
import de.gupta.clean.crud.template.useCases.operationOLD.quarantine.domain.model.MutationReplayInputs;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/mutation-quarantines")
public class DefaultSpringRestMutationQuarantineController
		extends AbstractSpringRestQuarantineController<MutationReplayInputs>
		implements SpringRestQuarantineController
{
	public DefaultSpringRestMutationQuarantineController(
			final QuarantineApplicationController<MutationReplayInputs> applicationController,
			final QuarantineWebMapper webMapper)
	{
		super(applicationController, webMapper);
	}
}