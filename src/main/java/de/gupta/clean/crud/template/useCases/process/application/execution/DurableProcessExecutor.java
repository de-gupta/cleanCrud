package de.gupta.clean.crud.template.useCases.process.application.execution;

import de.gupta.clean.crud.template.useCases.process.domain.definition.DurableProcessPayload;
import de.gupta.clean.crud.template.useCases.process.domain.model.outcome.DurableProcessOutcome;

@FunctionalInterface
public interface DurableProcessExecutor<Payload extends DurableProcessPayload>
{
	DurableProcessOutcome execute(Payload payload, DurableProcessExecutionContext context);
}
