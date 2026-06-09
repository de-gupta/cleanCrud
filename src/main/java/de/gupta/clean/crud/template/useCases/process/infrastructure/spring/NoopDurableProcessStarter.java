package de.gupta.clean.crud.template.useCases.process.infrastructure.spring;

import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStartRequest;
import de.gupta.clean.crud.template.useCases.process.application.registration.DurableProcessStarter;
import de.gupta.clean.crud.template.useCases.process.domain.model.id.DurableProcessTaskId;

final class NoopDurableProcessStarter implements DurableProcessStarter
{
	static NoopDurableProcessStarter create()
	{
		return new NoopDurableProcessStarter();
	}

	@Override
	public DurableProcessTaskId start(final DurableProcessStartRequest<?, ?> startRequest)
	{
		return DurableProcessTaskId.random();
	}

	private NoopDurableProcessStarter()
	{
	}
}
