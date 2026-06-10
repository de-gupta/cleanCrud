package de.gupta.clean.crud.template.useCases.operation.creation.domain.policy;

import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.evaluation.CreationPolicyDecision;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.profile.CreationPolicyProfileResolver;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.quarantine.CreationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.violation.CreationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.violation.CreationViolationHandling;
import de.gupta.clean.crud.template.useCases.operation.creation.domain.policy.violation.CreationViolationKind;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.invariant.InvariantSeverity;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.invariant.InvariantViolation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreationPolicyContractsTest
{
	@Test
	void defaultResolverMapsAuthoritativeEventsToQuarantineCapableProfile()
	{
		var profile = CreationPolicyProfileResolver.defaultResolver()
		                                           .resolve(OperationSource.AUTHORITATIVE_EXTERNAL_EVENT);

		assertEquals(CreationViolationHandling.ALLOW, profile.accessViolationHandling());
		assertEquals(CreationViolationHandling.QUARANTINE, profile.hardInvariantViolationHandling());
		assertEquals(CreationViolationHandling.QUARANTINE, profile.externalConsistencyViolationHandling());
	}

	@Test
	void quarantineDecisionCarriesStructuredRequest()
	{
		var request = new CreationQuarantineRequest(
				OperationSource.AUTHORITATIVE_EXTERNAL_EVENT,
				List.of(CreationPolicyViolation.externalConsistency("Broker rejected create")));
		var decision = CreationPolicyDecision.quarantine(request);

		assertFalse(decision.allowed());
		assertTrue(decision.quarantined());
		assertEquals(CreationViolationKind.EXTERNAL_CONSISTENCY,
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