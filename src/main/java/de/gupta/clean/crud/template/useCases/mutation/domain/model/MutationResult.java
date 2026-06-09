package de.gupta.clean.crud.template.useCases.mutation.domain.model;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.evaluation.MutationPolicyDecision;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.quarantine.MutationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.quarantine.QuarantinedMutationException;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation.MutationPolicyViolation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MutationResult<DomainId, DomainModel>(
		MutationContext<DomainId, DomainModel> context,
		MutationPolicyDecision policyDecision,
		Optional<IdentifiedModel<DomainId, DomainModel>> updated)
{
	public static <DomainId, DomainModel> MutationResult<DomainId, DomainModel> applied(
			final MutationContext<DomainId, DomainModel> context,
			final MutationPolicyDecision policyDecision,
			final IdentifiedModel<DomainId, DomainModel> updated)
	{
		return new MutationResult<>(context, policyDecision, Optional.of(updated));
	}

	public static <DomainId, DomainModel> MutationResult<DomainId, DomainModel> quarantined(
			final MutationContext<DomainId, DomainModel> context,
			final MutationPolicyDecision policyDecision)
	{
		return new MutationResult<>(context, policyDecision, Optional.empty());
	}

	public MutationResult
	{
		Objects.requireNonNull(context, "context");
		Objects.requireNonNull(policyDecision, "policyDecision");
		Objects.requireNonNull(updated, "updated");
		if (policyDecision.allowed() && updated.isEmpty())
		{
			throw new IllegalArgumentException("Allowed mutation results must carry an updated model");
		}
		if (policyDecision.quarantined() && updated.isPresent())
		{
			throw new IllegalArgumentException("Quarantined mutation results must not carry an updated model");
		}
	}

	public boolean applied()
	{
		return updated.isPresent();
	}

	public boolean quarantined()
	{
		return policyDecision.quarantined();
	}

	public Optional<MutationQuarantineRequest> quarantineRequest()
	{
		return policyDecision.quarantineRequest();
	}

	public List<MutationPolicyViolation> toleratedViolations()
	{
		return policyDecision.toleratedViolations();
	}

	public IdentifiedModel<DomainId, DomainModel> updatedOrThrow()
	{
		return updated.orElseThrow(() -> quarantineRequest()
				.<RuntimeException>map(QuarantinedMutationException::withRequest)
				.orElseGet(() -> new IllegalStateException("Mutation result did not carry an updated model")));
	}
}
