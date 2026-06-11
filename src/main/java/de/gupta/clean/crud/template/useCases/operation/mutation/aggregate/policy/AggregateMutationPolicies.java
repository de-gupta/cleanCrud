package de.gupta.clean.crud.template.useCases.operation.mutation.aggregate.policy;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.operation.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.violation.OperationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operation.domain.policy.violation.ViolationHandling;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.evaluation.MutationPolicyBundle;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.evaluation.MutationPolicyDecision;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.evaluation.SourceAwareMutationPolicy;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.quarantine.MutationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operation.mutation.domain.policy.quarantine.QuarantinedMutationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;

public final class AggregateMutationPolicies
{
	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	SourceAwareMutationPolicy<DomainModel> sourceAwarePolicy(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition)
	{
		return new EvaluatingSourceAwareMutationPolicy<>(policyBundle(definition));
	}

	private static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	MutationPolicyBundle<DomainModel> policyBundle(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition)
	{
		return new MutationPolicyBundle<>(
				definition.mutationPolicyProfileResolver(),
				definition.mutationAccessPolicy(),
				definition.mutationTransitionPolicy(),
				definition.domainInvariantPolicy(),
				definition.externalConsistencyPolicy());
	}

	private AggregateMutationPolicies()
	{
	}

	private record EvaluatingSourceAwareMutationPolicy<DomainModel>(MutationPolicyBundle<DomainModel> policyBundle)
			implements SourceAwareMutationPolicy<DomainModel>
	{
		private static final Logger log =
				LoggerFactory.getLogger(EvaluatingSourceAwareMutationPolicy.class);

		@Override
		public MutationPolicyDecision evaluate(
				final OperationSource source,
				final DomainModel beforeModel,
				final DomainModel afterModel)
		{
			var profile = policyBundle.profileResolver().resolve(source);
			var toleratedViolations = new ArrayList<OperationPolicyViolation>();
			var quarantiningViolations = new ArrayList<OperationPolicyViolation>();
			var rejectingViolations = new ArrayList<OperationPolicyViolation>();
			collect(
					policyBundle.accessPolicy().accessViolationFor(source, beforeModel, afterModel)
					            .map(OperationPolicyViolation::access),
					profile.accessViolationHandling(),
					toleratedViolations,
					quarantiningViolations,
					rejectingViolations);
			collect(
					policyBundle.transitionPolicy().transitionViolationFor(source, beforeModel, afterModel)
					            .map(OperationPolicyViolation::core),
					profile.coreViolationHandling(),
					toleratedViolations,
					quarantiningViolations,
					rejectingViolations);
			for (var invariantViolation : policyBundle.invariantPolicy()
			                                          .invariantViolationsFor(source, beforeModel, afterModel))
			{
				var handling = switch (invariantViolation.severity())
				{
					case HARD -> profile.hardInvariantViolationHandling();
					case SOFT -> profile.softInvariantViolationHandling();
				};
				collect(
						Optional.of(OperationPolicyViolation.invariant(invariantViolation)),
						handling,
						toleratedViolations,
						quarantiningViolations,
						rejectingViolations);
			}
			collect(
					policyBundle.externalConsistencyPolicy()
					            .consistencyViolationFor(source, beforeModel, afterModel)
					            .map(OperationPolicyViolation::externalConsistency),
					profile.externalConsistencyViolationHandling(),
					toleratedViolations,
					quarantiningViolations,
					rejectingViolations);
			if (!quarantiningViolations.isEmpty())
			{
				return MutationPolicyDecision.quarantine(new MutationQuarantineRequest(
						source,
						quarantiningViolations), toleratedViolations);
			}
			if (!rejectingViolations.isEmpty())
			{
				throw rejectionFor(rejectingViolations.getFirst());
			}
			return MutationPolicyDecision.allow(toleratedViolations);
		}

		@Override
		public void validate(
				final OperationSource source,
				final DomainModel beforeModel,
				final DomainModel afterModel)
		{
			var decision = evaluate(source, beforeModel, afterModel);
			if (decision.quarantined())
			{
				var request = decision.quarantineRequest().orElseThrow();
				log.warn("Mutation quarantine stub engaged for source {} with violations {}", source,
						request.violations());
				throw QuarantinedMutationException.withRequest(request);
			}
		}

		private void collect(
				final Optional<OperationPolicyViolation> violation,
				final ViolationHandling handling,
				final ArrayList<OperationPolicyViolation> toleratedViolations,
				final ArrayList<OperationPolicyViolation> quarantiningViolations,
				final ArrayList<OperationPolicyViolation> rejectingViolations)
		{
			violation.ifPresent(candidate ->
			{
				switch (handling)
				{
					case ALLOW -> toleratedViolations.add(candidate);
					case REJECT -> rejectingViolations.add(candidate);
					case QUARANTINE -> quarantiningViolations.add(candidate);
				}
			});
		}

		private RuntimeException rejectionFor(final OperationPolicyViolation violation)
		{
			return switch (violation.kind())
			{
				case ACCESS -> AccessDeniedException.withMessage(violation.message());
				case CORE, INVARIANT, EXTERNAL_CONSISTENCY ->
						InvalidRequestException.withMessage(violation.message());
			};
		}
	}
}
