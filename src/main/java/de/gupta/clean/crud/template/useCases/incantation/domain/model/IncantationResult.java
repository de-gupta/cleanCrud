package de.gupta.clean.crud.template.useCases.incantation.domain.model;

import de.gupta.clean.crud.template.useCases.incantation.domain.policy.evaluation.IncantationPolicyDecision;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.quarantine.IncantationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.quarantine.QuarantinedIncantationException;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.violation.IncantationPolicyViolation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record IncantationResult<DomainId, DomainModel>(
		IncantationContext<DomainId, DomainModel> context,
		IncantationPolicyDecision policyDecision,
		Optional<CreateResult<DomainId, DomainModel>> created)
{
	public static <DomainId, DomainModel> IncantationResult<DomainId, DomainModel> created(
			final IncantationContext<DomainId, DomainModel> context,
			final IncantationPolicyDecision policyDecision,
			final CreateResult<DomainId, DomainModel> created)
	{
		return new IncantationResult<>(context, policyDecision, Optional.of(created));
	}

	public static <DomainId, DomainModel> IncantationResult<DomainId, DomainModel> quarantined(
			final IncantationContext<DomainId, DomainModel> context,
			final IncantationPolicyDecision policyDecision)
	{
		return new IncantationResult<>(context, policyDecision, Optional.empty());
	}

	public IncantationResult
	{
		Objects.requireNonNull(context, "context");
		Objects.requireNonNull(policyDecision, "policyDecision");
		Objects.requireNonNull(created, "created");
		if (policyDecision.allowed() && created.isEmpty())
		{
			throw new IllegalArgumentException("Allowed incantation results must carry a create result");
		}
		if (policyDecision.quarantined() && created.isPresent())
		{
			throw new IllegalArgumentException("Quarantined incantation results must not carry a create result");
		}
	}

	public boolean creationApplied()
	{
		return created.isPresent();
	}

	public boolean quarantined()
	{
		return policyDecision.quarantined();
	}

	public Optional<IncantationQuarantineRequest> quarantineRequest()
	{
		return policyDecision.quarantineRequest();
	}

	public List<IncantationPolicyViolation> toleratedViolations()
	{
		return policyDecision.toleratedViolations();
	}

	public CreateResult<DomainId, DomainModel> createdOrThrow()
	{
		return created.orElseThrow(() -> quarantineRequest()
				.<RuntimeException>map(QuarantinedIncantationException::withRequest)
				.orElseGet(() -> new IllegalStateException("Incantation result did not carry a create result")));
	}
}
