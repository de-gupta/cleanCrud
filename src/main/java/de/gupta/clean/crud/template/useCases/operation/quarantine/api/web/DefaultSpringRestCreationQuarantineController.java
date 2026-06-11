package de.gupta.clean.crud.template.useCases.operation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operation.quarantine.api.application.QuarantineApplicationController;
import de.gupta.clean.crud.template.useCases.operation.quarantine.domain.model.CreationReplayInputs;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/creation-quarantines")
public class DefaultSpringRestCreationQuarantineController
		extends AbstractSpringRestQuarantineController<CreationReplayInputs>
		implements SpringRestQuarantineController
{
	public DefaultSpringRestCreationQuarantineController(
			final QuarantineApplicationController<CreationReplayInputs> applicationController,
			final QuarantineWebMapper webMapper)
	{
		super(applicationController, webMapper);
	}
}
