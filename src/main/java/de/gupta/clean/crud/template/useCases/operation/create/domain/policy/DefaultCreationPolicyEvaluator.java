package de.gupta.clean.crud.template.useCases.operation.create.domain.policy;

import de.gupta.clean.crud.template.useCases.operation.create.domain.attempt.PreparedCreationAttempt;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationViolation;

import java.util.*;

final class DefaultCreationPolicyEvaluator<DomainModel> implements CreationPolicyEvaluator<DomainModel>
{
	private final List<CreationPolicy<DomainModel>> policies;

	static <DomainModel> CreationPolicyEvaluator<DomainModel> with(
			final Collection<? extends CreationPolicy<DomainModel>> policies)
	{
		return new DefaultCreationPolicyEvaluator<>(policies);
	}

	@Override
	public CreationPolicyEvaluation evaluate(final PreparedCreationAttempt<?, DomainModel> preparedAttempt)
	{
		Objects.requireNonNull(preparedAttempt, "preparedAttempt");

		var blockingViolations = new ArrayList<CreationOperationViolation>();
		var toleratedViolations = new ArrayList<CreationOperationViolation>();
		var decision = CreationDecision.ALLOW;
		var quarantineReference = Optional.<String>empty();

		for (var policy : policies)
		{
			var evaluation = policy.evaluate(preparedAttempt);
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

	private DefaultCreationPolicyEvaluator(final Collection<? extends CreationPolicy<DomainModel>> policies)
	{
		Objects.requireNonNull(policies, "policies");
		this.policies = List.copyOf(policies);
	}
}