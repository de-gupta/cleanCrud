package de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy;

import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.invariant.InvariantViolation;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.evaluation.MutationPolicyDecision;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.profile.MutationPolicyProfileResolver;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.quarantine.MutationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.violation.MutationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.violation.MutationViolationHandling;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.violation.MutationViolationKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MutationPolicyContractsTest
{
	@Test
	void defaultResolverMapsAuthoritativeEventsToQuarantineCapableProfile()
	{
		var profile = MutationPolicyProfileResolver.defaultResolver()
		                                           .resolve(OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);

		assertThat(profile.accessViolationHandling())
				.as("authoritative events should allow access violations")
				.isEqualTo(MutationViolationHandling.ALLOW);
		assertThat(profile.hardInvariantViolationHandling())
				.as("authoritative events should quarantine hard invariant violations")
				.isEqualTo(MutationViolationHandling.QUARANTINE);
		assertThat(profile.externalConsistencyViolationHandling())
				.as("authoritative events should quarantine external consistency violations")
				.isEqualTo(MutationViolationHandling.QUARANTINE);
	}

	@Test
	void quarantineDecisionCarriesStructuredRequest()
	{
		var request = new MutationQuarantineRequest(
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
				List.of(MutationPolicyViolation.externalConsistency("Broker state inconsistent")));
		var decision = MutationPolicyDecision.quarantine(request);

		assertThat(decision.allowed())
				.as("quarantined decision should not be allowed")
				.isFalse();
		assertThat(decision.quarantined())
				.as("quarantined decision should report quarantined")
				.isTrue();
		assertThat(decision.quarantineRequest().orElseThrow().violations().getFirst().kind())
				.as("quarantine request should carry the violation kind")
				.isEqualTo(MutationViolationKind.EXTERNAL_CONSISTENCY);
	}

	@Test
	void invariantViolationHelpersCaptureSeverity()
	{
		var hard = InvariantViolation.hard("hard");
		var soft = InvariantViolation.soft("soft");

		assertThat(hard.severity())
				.as("hard() should produce HARD severity")
				.isEqualTo(InvariantSeverity.HARD);
		assertThat(soft.severity())
				.as("soft() should produce SOFT severity")
				.isEqualTo(InvariantSeverity.SOFT);
	}
}
