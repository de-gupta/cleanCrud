package de.gupta.clean.crud.template.useCases.incantation.domain.policy;

import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationSource;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.evaluation.IncantationPolicyDecision;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.profile.IncantationPolicyProfileResolver;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.quarantine.IncantationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.violation.IncantationPolicyViolation;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.violation.IncantationViolationHandling;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.violation.IncantationViolationKind;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.invariant.InvariantViolation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IncantationPolicyContractsTest
{
	@Test
	void defaultResolverMapsAuthoritativeEventsToQuarantineCapableProfile()
	{
		var profile = IncantationPolicyProfileResolver.defaultResolver()
		                                              .resolve(IncantationSource.AUTHORITATIVE_EXTERNAL_EVENT);

		assertEquals(IncantationViolationHandling.ALLOW, profile.accessViolationHandling());
		assertEquals(IncantationViolationHandling.QUARANTINE, profile.hardInvariantViolationHandling());
		assertEquals(IncantationViolationHandling.QUARANTINE, profile.externalConsistencyViolationHandling());
	}

	@Test
	void quarantineDecisionCarriesStructuredRequest()
	{
		var request = new IncantationQuarantineRequest(
				IncantationSource.AUTHORITATIVE_EXTERNAL_EVENT,
				List.of(IncantationPolicyViolation.externalConsistency("Broker rejected create")));
		var decision = IncantationPolicyDecision.quarantine(request);

		assertFalse(decision.allowed());
		assertTrue(decision.quarantined());
		assertEquals(IncantationViolationKind.EXTERNAL_CONSISTENCY,
				decision.quarantineRequest().orElseThrow().violations().getFirst().kind());
	}

	@Test
	void invariantHelpersCaptureSeverity()
	{
		var hard = InvariantViolation.hard("hard");
		var soft = InvariantViolation.soft("soft");

		assertEquals(InvariantSeverity.HARD, hard.severity());
		assertEquals(InvariantSeverity.SOFT, soft.severity());
	}
}