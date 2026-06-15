package de.gupta.clean.crud.template.useCases.operation.create.domain.policy;

import de.gupta.clean.crud.template.useCases.operation.create.domain.result.CreateOperationViolation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CreationPolicyEvaluation(
		CreationDecision decision,
		Collection<CreateOperationViolation> blockingViolations,
		Collection<CreateOperationViolation> toleratedViolations,
		Optional<String> quarantineReference)
{
	public static CreationPolicyEvaluation allow()
	{
		return allow(List.of());
	}

	public static CreationPolicyEvaluation allow(final Collection<CreateOperationViolation> toleratedViolations)
	{
		return new CreationPolicyEvaluation(CreationDecision.ALLOW, List.of(), toleratedViolations, Optional.empty());
	}

	public static CreationPolicyEvaluation reject(final Collection<CreateOperationViolation> blockingViolations)
	{
		return reject(blockingViolations, List.of());
	}

	public static CreationPolicyEvaluation reject(
			final Collection<CreateOperationViolation> blockingViolations,
			final Collection<CreateOperationViolation> toleratedViolations)
	{
		return new CreationPolicyEvaluation(
				CreationDecision.REJECT,
				blockingViolations,
				toleratedViolations,
				Optional.empty());
	}

	public static CreationPolicyEvaluation quarantine(final Collection<CreateOperationViolation> blockingViolations)
	{
		return quarantine(blockingViolations, List.of(), Optional.empty());
	}

	public static CreationPolicyEvaluation quarantine(
			final Collection<CreateOperationViolation> blockingViolations,
			final Collection<CreateOperationViolation> toleratedViolations,
			final Optional<String> quarantineReference)
	{
		return new CreationPolicyEvaluation(
				CreationDecision.QUARANTINE,
				blockingViolations,
				toleratedViolations,
				quarantineReference);
	}

	public static CreationPolicyEvaluation quarantine(
			final Collection<CreateOperationViolation> blockingViolations,
			final Collection<CreateOperationViolation> toleratedViolations)
	{
		return quarantine(blockingViolations, toleratedViolations, Optional.empty());
	}

	public CreationPolicyEvaluation
	{
		Objects.requireNonNull(decision, "decision");
		blockingViolations = List.copyOf(Objects.requireNonNull(blockingViolations, "blockingViolations"));
		toleratedViolations = List.copyOf(Objects.requireNonNull(toleratedViolations, "toleratedViolations"));
		quarantineReference = Optional.ofNullable(quarantineReference).orElse(Optional.empty());

		switch (decision)
		{
			case ALLOW ->
			{
				if (!blockingViolations.isEmpty())
				{
					throw new IllegalArgumentException("ALLOW decisions must not carry blocking violations");
				}
				if (quarantineReference.isPresent())
				{
					throw new IllegalArgumentException("ALLOW decisions must not carry quarantine references");
				}
			}
			case REJECT ->
			{
				if (blockingViolations.isEmpty())
				{
					throw new IllegalArgumentException("REJECT decisions must carry blocking violations");
				}
				if (quarantineReference.isPresent())
				{
					throw new IllegalArgumentException("REJECT decisions must not carry quarantine references");
				}
			}
			case QUARANTINE ->
			{
				if (blockingViolations.isEmpty())
				{
					throw new IllegalArgumentException("QUARANTINE decisions must carry blocking violations");
				}
			}
		}
	}
}