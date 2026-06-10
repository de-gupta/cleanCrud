package de.gupta.clean.crud.template.useCases.incantation.aggregate.policy;

import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.useCases.crud.aggregate.definition.AggregateCrudDefinition;
import de.gupta.clean.crud.template.useCases.crud.aggregate.engine.SatelliteCreateValidator;
import de.gupta.clean.crud.template.useCases.incantation.domain.model.IncantationSource;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.evaluation.IncantationPolicyBundle;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.evaluation.IncantationPolicyDecision;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.evaluation.SourceAwareIncantationPolicy;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.quarantine.IncantationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.quarantine.QuarantinedIncantationException;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.violation.IncantationPolicyViolation;
import de.gupta.clean.crud.template.useCases.incantation.domain.policy.violation.IncantationViolationHandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;

public final class AggregateIncantationPolicies
{
	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	SourceAwareIncantationPolicy<DomainModel> sourceAwarePolicy(
			final AggregateCrudDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition)
	{
		return new EvaluatingSourceAwareIncantationPolicy<>(new IncantationPolicyBundle<>(
				definition.incantationPolicyProfileResolver(),
				definition.incantationAccessPolicy(),
				definition.incantationCreationPolicy(),
				definition.incantationInvariantPolicy(),
				definition.incantationExternalConsistencyPolicy()));
	}

	public static SatelliteCreateValidator satelliteCreateValidator(final IncantationSource source)
	{
		return new SatelliteCreateValidator()
		{
			@Override
			public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
					SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
			void validate(
					final de.gupta.clean.crud.template.useCases.crud.aggregate.relationship.AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch> relationship,
					final SatelliteDomainModel satelliteDomainModel)
			{
				AggregateIncantationPolicies.sourceAwarePolicy(relationship.satelliteDefinition())
				                            .validate(source, satelliteDomainModel);
			}
		};
	}

	private AggregateIncantationPolicies()
	{
	}

	private record EvaluatingSourceAwareIncantationPolicy<DomainModel>(
			IncantationPolicyBundle<DomainModel> policyBundle)
			implements SourceAwareIncantationPolicy<DomainModel>
	{
		private static final Logger log =
				LoggerFactory.getLogger(EvaluatingSourceAwareIncantationPolicy.class);

		@Override
		public IncantationPolicyDecision evaluate(
				final IncantationSource source,
				final DomainModel afterModel)
		{
			var profile = policyBundle.profileResolver().resolve(source);
			var toleratedViolations = new ArrayList<IncantationPolicyViolation>();
			var quarantiningViolations = new ArrayList<IncantationPolicyViolation>();
			var rejectingViolations = new ArrayList<IncantationPolicyViolation>();
			collect(
					policyBundle.accessPolicy().accessViolationFor(source, afterModel)
					            .map(IncantationPolicyViolation::access),
					profile.accessViolationHandling(),
					toleratedViolations,
					quarantiningViolations,
					rejectingViolations);
			collect(
					policyBundle.creationPolicy().creationViolationFor(source, afterModel)
					            .map(IncantationPolicyViolation::creation),
					profile.creationViolationHandling(),
					toleratedViolations,
					quarantiningViolations,
					rejectingViolations);
			for (var invariantViolation : policyBundle.invariantPolicy().invariantViolationsFor(source, afterModel))
			{
				var handling = switch (invariantViolation.severity())
				{
					case HARD -> profile.hardInvariantViolationHandling();
					case SOFT -> profile.softInvariantViolationHandling();
				};
				collect(
						Optional.of(IncantationPolicyViolation.invariant(invariantViolation)),
						handling,
						toleratedViolations,
						quarantiningViolations,
						rejectingViolations);
			}
			collect(
					policyBundle.externalConsistencyPolicy().consistencyViolationFor(source, afterModel)
					            .map(IncantationPolicyViolation::externalConsistency),
					profile.externalConsistencyViolationHandling(),
					toleratedViolations,
					quarantiningViolations,
					rejectingViolations);
			if (!quarantiningViolations.isEmpty())
			{
				return IncantationPolicyDecision.quarantine(
						new IncantationQuarantineRequest(source, quarantiningViolations),
						toleratedViolations);
			}
			if (!rejectingViolations.isEmpty())
			{
				throw rejectionFor(rejectingViolations.getFirst());
			}
			return IncantationPolicyDecision.allow(toleratedViolations);
		}

		public void validate(final IncantationSource source, final DomainModel afterModel)
		{
			var decision = evaluate(source, afterModel);
			if (decision.quarantined())
			{
				var request = decision.quarantineRequest().orElseThrow();
				log.warn("Incantation quarantine stub engaged for source {} with violations {}", source,
						request.violations());
				throw QuarantinedIncantationException.withRequest(request);
			}
		}

		private void collect(
				final Optional<IncantationPolicyViolation> violation,
				final IncantationViolationHandling handling,
				final ArrayList<IncantationPolicyViolation> toleratedViolations,
				final ArrayList<IncantationPolicyViolation> quarantiningViolations,
				final ArrayList<IncantationPolicyViolation> rejectingViolations)
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

		private RuntimeException rejectionFor(final IncantationPolicyViolation violation)
		{
			return switch (violation.kind())
			{
				case ACCESS -> AccessDeniedException.withMessage(violation.message());
				case CREATION, INVARIANT, EXTERNAL_CONSISTENCY ->
						InvalidRequestException.withMessage(violation.message());
			};
		}
	}
}
