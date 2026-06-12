package de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.model;

import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.evaluation.CreationPolicyDecision;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.quarantine.CreationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.quarantine.QuarantinedCreationException;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.violation.OperationPolicyViolation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CreationResult<DomainId, DomainModel>(
		CreationContext<DomainId, DomainModel> context,
		CreationPolicyDecision policyDecision,
		Optional<CreateResult<DomainId, DomainModel>> created)
{
	public static <DomainId, DomainModel> CreationResult<DomainId, DomainModel> created(
			final CreationContext<DomainId, DomainModel> context,
			final CreationPolicyDecision policyDecision,
			final CreateResult<DomainId, DomainModel> created)
	{
		return new CreationResult<>(context, policyDecision, Optional.of(created));
	}

	public static <DomainId, DomainModel> CreationResult<DomainId, DomainModel> quarantined(
			final CreationContext<DomainId, DomainModel> context,
			final CreationPolicyDecision policyDecision)
	{
		return new CreationResult<>(context, policyDecision, Optional.empty());
	}

	public CreationResult
	{
		Objects.requireNonNull(context, "context");
		Objects.requireNonNull(policyDecision, "policyDecision");
		Objects.requireNonNull(created, "created");
		if (policyDecision.allowed() && created.isEmpty())
		{
			throw new IllegalArgumentException("Allowed creation results must carry a create result");
		}
		if (policyDecision.quarantined() && created.isPresent())
		{
			throw new IllegalArgumentException("Quarantined creation results must not carry a create result");
		}
	}

	public boolean applied()
	{
		return created.isPresent();
	}

	public boolean quarantined()
	{
		return policyDecision.quarantined();
	}

	public Optional<CreationQuarantineRequest> quarantineRequest()
	{
		return policyDecision.quarantineRequest();
	}

	public List<OperationPolicyViolation> toleratedViolations()
	{
		return policyDecision.toleratedViolations();
	}

	public CreateResult<DomainId, DomainModel> createdOrThrow()
	{
		return created.orElseThrow(() -> quarantineRequest()
				.<RuntimeException>map(QuarantinedCreationException::withRequest)
				.orElseGet(() -> new IllegalStateException("Creation result did not carry a create result")));
	}
}