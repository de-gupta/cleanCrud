package de.gupta.clean.crud.template.useCases.crud.aggregate.engine;

@FunctionalInterface
public interface AggregateLifecycleEngine
{
	<Result> Result execute(CrudWorkflow<Result> workflow);
}