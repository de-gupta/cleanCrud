package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.web;

import de.gupta.clean.crud.template.useCases.operation.creation.quarantine.api.application.CreationQuarantineApplicationController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/creation-quarantines")
public class DefaultSpringRestCreationQuarantineController
		extends AbstractSpringRestCreationQuarantineController
		implements SpringRestCreationQuarantineController
{
	public DefaultSpringRestCreationQuarantineController(
			final CreationQuarantineApplicationController applicationController,
			final CreationQuarantineWebMapper webMapper)
	{
		super(applicationController, webMapper);
	}
}
