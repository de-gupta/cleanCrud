package de.gupta.clean.crud.template.useCases.operation.create.domain.policy;

import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationViolation;

import java.util.*;

final class DefaultCreationPolicyEvaluator implements CreationPolicyEvaluator
{
	private final List<CreationPolicy> policies;

	@Override
	public CreationPolicyEvaluation evaluate(final CreationOperationRequest<?> request, final CreationPlan<?> plan)
	{
		Objects.requireNonNull(request, "request");
		Objects.requireNonNull(plan, "plan");

		var blockingViolations = new ArrayList<CreationOperationViolation>();
		var toleratedViolations = new ArrayList<CreationOperationViolation>();
		var decision = CreationDecision.ALLOW;
		var quarantineReference = Optional.<String>empty();

		for (var policy : policies)
		{
			var evaluation = policy.evaluate(request, plan);
			blockingViolations.addAll(evaluation.blockingViolations());
			toleratedViolations.addAll(evaluation.toleratedViolations());

			switch (evaluation.decision())
			{
				case ALLOW ->
				{
				}
				case REJECT ->
				{
					if (decision == CreationDecision.ALLOW)
					{
						decision = CreationDecision.REJECT;
					}
				}
				case QUARANTINE ->
				{
					decision = CreationDecision.QUARANTINE;
					if (evaluation.quarantineReference().isPresent())
					{
						quarantineReference = evaluation.quarantineReference();
					}
				}
			}
		}

		return switch (decision)
		{
			case ALLOW -> CreationPolicyEvaluation.allow(toleratedViolations);
			case REJECT -> CreationPolicyEvaluation.reject(blockingViolations, toleratedViolations);
			case QUARANTINE -> CreationPolicyEvaluation.quarantine(
					blockingViolations,
					toleratedViolations,
					quarantineReference);
		};
	}

	DefaultCreationPolicyEvaluator(final Collection<? extends CreationPolicy> policies)
	{
		Objects.requireNonNull(policies, "policies");
		this.policies = List.copyOf(policies);
	}
}