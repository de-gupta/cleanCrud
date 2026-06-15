package de.gupta.clean.crud.template.domain.aggregate.lifecycle;

@FunctionalInterface
public interface AggregateLifecycle
{
	<Result> Result execute(AggregateWorkflow<Result> workflow);
}