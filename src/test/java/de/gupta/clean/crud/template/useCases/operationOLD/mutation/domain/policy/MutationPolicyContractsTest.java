package de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy;

import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.invariant.InvariantViolation;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.violation.OperationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.violation.ViolationHandling;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.violation.ViolationKind;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.evaluation.MutationPolicyDecision;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.profile.MutationPolicyProfileResolver;
import de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.policy.quarantine.MutationQuarantineRequest;
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
				.isEqualTo(ViolationHandling.ALLOW);
		assertThat(profile.hardInvariantViolationHandling())
				.as("authoritative events should quarantine hard invariant violations")
				.isEqualTo(ViolationHandling.QUARANTINE);
		assertThat(profile.externalConsistencyViolationHandling())
				.as("authoritative events should quarantine external consistency violations")
				.isEqualTo(ViolationHandling.QUARANTINE);
	}

	@Test
	void quarantineDecisionCarriesStructuredRequest()
	{
		var request = new MutationQuarantineRequest(
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
				List.of(OperationPolicyViolation.externalConsistency("Broker state inconsistent")));
		var decision = MutationPolicyDecision.quarantine(request);

		assertThat(decision.allowed())
				.as("quarantined decision should not be allowed")
				.isFalse();
		assertThat(decision.quarantined())
				.as("quarantined decision should report quarantined")
				.isTrue();
		assertThat(decision.quarantineRequest().orElseThrow().violations().getFirst().kind())
				.as("quarantine request should carry the violation kind")
				.isEqualTo(ViolationKind.EXTERNAL_CONSISTENCY);
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