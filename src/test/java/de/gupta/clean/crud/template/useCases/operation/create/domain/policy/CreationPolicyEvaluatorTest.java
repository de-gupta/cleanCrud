package de.gupta.clean.crud.template.useCases.operation.create.domain.policy;

import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationRequestMetadata;
import de.gupta.clean.crud.template.useCases.operation.common.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreateOperationPayload;
import de.gupta.clean.crud.template.useCases.operation.create.domain.model.CreationOperationRequest;
import de.gupta.clean.crud.template.useCases.operation.create.domain.plan.CreationPlan;
import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreationOperationViolation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreationPolicyEvaluatorTest
{
	@Test
	void evaluate_returnsAllowDecision_whenNoBlockingViolationsExist()
	{
		var evaluation = CreationPolicyEvaluator.of(List.of((request, plan) -> CreationPolicyEvaluation.allow()))
		                                        .evaluate(request(), plan());

		assertThat(evaluation.decision()).isEqualTo(CreationDecision.ALLOW);
		assertThat(evaluation.blockingViolations()).isEmpty();
		assertThat(evaluation.toleratedViolations()).isEmpty();
		assertThat(evaluation.quarantineReference()).isEmpty();
	}

	@Test
	void evaluate_returnsAllowDecision_whenOnlyToleratedViolationsExist()
	{
		var tolerated = CreationOperationViolation.invariant("soft invariant warning");
		var evaluation = CreationPolicyEvaluator.of(List.of(
														(request, plan) -> CreationPolicyEvaluation.allow(List.of(
																tolerated))))
		                                        .evaluate(request(), plan());

		assertThat(evaluation.decision()).isEqualTo(CreationDecision.ALLOW);
		assertThat(evaluation.blockingViolations()).isEmpty();
		assertThat(evaluation.toleratedViolations()).containsExactly(tolerated);
	}

	@Test
	void evaluate_returnsRejectDecision_whenBlockingViolationsExist()
	{
		var blocking = CreationOperationViolation.core("core rule failed");
		var tolerated = CreationOperationViolation.invariant("soft warning");
		var evaluation = CreationPolicyEvaluator.of(List.of(
														(request, plan) -> CreationPolicyEvaluation.reject(
																List.of(blocking),
																List.of(tolerated))))
		                                        .evaluate(request(), plan());

		assertThat(evaluation.decision()).isEqualTo(CreationDecision.REJECT);
		assertThat(evaluation.blockingViolations()).containsExactly(blocking);
		assertThat(evaluation.toleratedViolations()).containsExactly(tolerated);
		assertThat(evaluation.quarantineReference()).isEmpty();
	}

	@Test
	void evaluate_returnsQuarantineDecision_whenQuarantineViolationsExist()
	{
		var blocking = CreationOperationViolation.access("manual review needed");
		var tolerated = CreationOperationViolation.externalConsistency("eventual consistency risk");
		var evaluation = CreationPolicyEvaluator.of(List.of(
														(request, plan) -> CreationPolicyEvaluation.quarantine(
																List.of(blocking),
																List.of(tolerated),
																Optional.of("Q-9"))))
		                                        .evaluate(request(), plan());

		assertThat(evaluation.decision()).isEqualTo(CreationDecision.QUARANTINE);
		assertThat(evaluation.blockingViolations()).containsExactly(blocking);
		assertThat(evaluation.toleratedViolations()).containsExactly(tolerated);
		assertThat(evaluation.quarantineReference()).contains("Q-9");
	}

	@Test
	void allowInvariant_rejectsBlockingViolations()
	{
		assertThatThrownBy(() -> new CreationPolicyEvaluation(
				CreationDecision.ALLOW,
				List.of(CreationOperationViolation.core("blocked")),
				List.of(),
				Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("ALLOW decisions");
	}

	private static CreationOperationRequest<TestPayload> request()
	{
		return new CreationOperationRequest<>(
				new TestPayload("draft"),
				OperationRequestMetadata.source(OperationSource.USER_INTENT));
	}

	private static CreationPlan<String> plan()
	{
		return CreationPlan.of("aggregate.Task", "draft");
	}

	private record TestPayload(String name) implements CreateOperationPayload
	{
	}
}