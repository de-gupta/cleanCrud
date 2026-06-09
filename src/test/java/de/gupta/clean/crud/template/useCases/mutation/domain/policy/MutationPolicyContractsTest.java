package de.gupta.clean.crud.template.useCases.mutation.domain.policy;

import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.evaluation.MutationPolicyDecision;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant.InvariantViolation;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.profile.MutationPolicyProfileResolver;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.quarantine.MutationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation.MutationPolicyViolation;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation.MutationViolationHandling;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation.MutationViolationKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MutationPolicyContractsTest
{
	@Test
	void defaultResolverMapsAuthoritativeEventsToQuarantineCapableProfile()
	{
		var profile = MutationPolicyProfileResolver.defaultResolver()
		                                           .resolve(MutationSource.AUTHORITATIVE_EXTERNAL_EVENT);

		assertEquals(MutationViolationHandling.ALLOW, profile.accessViolationHandling());
		assertEquals(MutationViolationHandling.QUARANTINE, profile.hardInvariantViolationHandling());
		assertEquals(MutationViolationHandling.QUARANTINE, profile.externalConsistencyViolationHandling());
	}

	@Test
	void quarantineDecisionCarriesStructuredRequest()
	{
		var request = new MutationQuarantineRequest(
				MutationSource.AUTHORITATIVE_EXTERNAL_EVENT,
				List.of(MutationPolicyViolation.externalConsistency("Broker state inconsistent")));
		var decision = MutationPolicyDecision.quarantine(request);

		assertFalse(decision.allowed());
		assertTrue(decision.quarantined());
		assertEquals(MutationViolationKind.EXTERNAL_CONSISTENCY,
				decision.quarantineRequest().orElseThrow().violations().getFirst().kind());
	}

	@Test
	void invariantViolationHelpersCaptureSeverity()
	{
		var hard = InvariantViolation.hard("hard");
		var soft = InvariantViolation.soft("soft");

		assertEquals(InvariantSeverity.HARD, hard.severity());
		assertEquals(InvariantSeverity.SOFT, soft.severity());
	}
}