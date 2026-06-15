package de.gupta.clean.crud.template.domain.aggregate.runtime;

import de.gupta.clean.crud.template.domain.aggregate.workflow.AggregateWorkflow;

@FunctionalInterface
public interface AggregateWorkflowRunner
{
	<Result> Result run(AggregateWorkflow<Result> workflow);
}