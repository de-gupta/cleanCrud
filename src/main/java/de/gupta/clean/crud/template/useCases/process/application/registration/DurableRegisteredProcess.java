package de.gupta.clean.crud.template.useCases.process.application.registration;

import de.gupta.clean.crud.template.useCases.process.application.execution.DurableProcessExecutor;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessDefinition;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessTrigger;

import java.util.Objects;

public record DurableRegisteredProcess<
		Trigger extends DurableProcessTrigger,
		Payload extends DurableProcessPayload>(
		DurableProcessDefinition<Trigger, Payload> definition,
		DurableProcessExecutor<Payload> executor)
{
	public DurableRegisteredProcess
	{
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(executor, "executor");
	}
}
