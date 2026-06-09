package de.gupta.clean.crud.template.useCases.mutation.aggregate.policy;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.mutation.domain.model.MutationSource;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.evaluation.MutationPolicyBundle;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.evaluation.MutationPolicyDecision;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.evaluation.SourceAwareMutationPolicy;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.quarantine.MutationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.quarantine.QuarantinedMutationException;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation.MutationPolicyViolation;
import de.gupta.clean.crud.template.useCases.mutation.domain.policy.violation.MutationViolationHandling;
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
				final MutationSource source,
				final DomainModel beforeModel,
				final DomainModel afterModel)
		{
			var profile = policyBundle.profileResolver().resolve(source);
			var toleratedViolations = new ArrayList<MutationPolicyViolation>();
			var quarantiningViolations = new ArrayList<MutationPolicyViolation>();
			var rejectingViolations = new ArrayList<MutationPolicyViolation>();
			collect(
					policyBundle.accessPolicy().accessViolationFor(source, beforeModel, afterModel)
					            .map(MutationPolicyViolation::access),
					profile.accessViolationHandling(),
					toleratedViolations,
					quarantiningViolations,
					rejectingViolations);
			collect(
					policyBundle.transitionPolicy().transitionViolationFor(source, beforeModel, afterModel)
					            .map(MutationPolicyViolation::transition),
					profile.transitionViolationHandling(),
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
						Optional.of(MutationPolicyViolation.invariant(invariantViolation)),
						handling,
						toleratedViolations,
						quarantiningViolations,
						rejectingViolations);
			}
			collect(
					policyBundle.externalConsistencyPolicy()
					            .consistencyViolationFor(source, beforeModel, afterModel)
					            .map(MutationPolicyViolation::externalConsistency),
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
				final MutationSource source,
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
				final Optional<MutationPolicyViolation> violation,
				final MutationViolationHandling handling,
				final ArrayList<MutationPolicyViolation> toleratedViolations,
				final ArrayList<MutationPolicyViolation> quarantiningViolations,
				final ArrayList<MutationPolicyViolation> rejectingViolations)
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

		private RuntimeException rejectionFor(final MutationPolicyViolation violation)
		{
			return switch (violation.kind())
			{
				case ACCESS -> AccessDeniedException.withMessage(violation.message());
				case TRANSITION, INVARIANT, EXTERNAL_CONSISTENCY ->
						InvalidRequestException.withMessage(violation.message());
			};
		}
	}
}
