package de.gupta.clean.crud.template.useCases.operation.creation.domain.policy;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.evaluation.CreationPolicyDecision;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.profile.CreationPolicyProfileResolver;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine.CreationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.invariant.InvariantViolation;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.violation.OperationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.violation.ViolationHandling;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.violation.ViolationKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreationPolicyContractsTest
{
	@Test
	void defaultResolverMapsAuthoritativeEventsToQuarantineCapableProfile()
	{
		var profile = CreationPolicyProfileResolver.defaultResolver()
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
		var request = new CreationQuarantineRequest(
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
				List.of(OperationPolicyViolation.externalConsistency("Broker rejected create")));
		var decision = CreationPolicyDecision.quarantine(request);

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
	void invariantHelpersCaptureSeverity()
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