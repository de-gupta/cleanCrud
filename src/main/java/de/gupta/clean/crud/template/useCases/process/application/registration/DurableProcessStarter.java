package de.gupta.clean.crud.template.useCases.process.application.registration;

import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;

public interface DurableProcessStarter
{
	DurableProcessTaskId start(DurableProcessStartRequest<?, ?> startRequest);
}
