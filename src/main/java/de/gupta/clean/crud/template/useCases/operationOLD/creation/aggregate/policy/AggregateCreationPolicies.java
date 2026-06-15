package de.gupta.clean.crud.template.useCases.operationOLD.creation.aggregate.policy;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.domain.model.exceptions.operation.InvalidRequestException;
import de.gupta.clean.crud.template.domain.model.exceptions.security.AccessDeniedException;
import de.gupta.clean.crud.template.domain.service.aggregate.mechanics.validator.SatelliteCreateValidator;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.evaluation.CreationPolicyBundle;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.evaluation.CreationPolicyDecision;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.evaluation.SourceAwareCreationPolicy;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.quarantine.CreationQuarantineRequest;
import de.gupta.clean.crud.template.useCases.operationOLD.creation.domain.policy.quarantine.QuarantinedCreationException;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.model.OperationSource;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.violation.OperationPolicyViolation;
import de.gupta.clean.crud.template.useCases.operationOLD.domain.policy.violation.ViolationHandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;

public enum AggregateCreationPolicies
{
	;

	public static SatelliteCreateValidator satelliteCreateValidator(final OperationSource source)
	{
		return new SatelliteCreateValidator()
		{
			@Override
			public <MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch,
					SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch>
			void validate(
					final AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate, MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate, SatelliteDomainModelUpdatePatch> relationship,
					final SatelliteDomainModel satelliteDomainModel)
			{
				AggregateCreationPolicies.sourceAwarePolicy(relationship.satelliteDefinition())
				                         .validate(source, satelliteDomainModel);
			}
		};
	}

	public static <DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch, DomainModelResponse>
	SourceAwareCreationPolicy<DomainModel> sourceAwarePolicy(
			final AggregateDefinition<DomainId, DomainModel, DomainModelCreate, DomainModelUpdatePatch,
					DomainModelResponse> definition)
	{
		return new EvaluatingSourceAwareCreationPolicy<>(new CreationPolicyBundle<>(
				definition.creationPolicyProfileResolver(),
				definition.creationAccessPolicy(),
				definition.creationPolicy(),
				definition.creationInvariantPolicy(),
				definition.creationExternalConsistencyPolicy()));
	}

	private record EvaluatingSourceAwareCreationPolicy<DomainModel>(
			CreationPolicyBundle<DomainModel> policyBundle)
			implements SourceAwareCreationPolicy<DomainModel>
	{
		private static final Logger log =
				LoggerFactory.getLogger(EvaluatingSourceAwareCreationPolicy.class);

		@Override
		public CreationPolicyDecision evaluate(
				final OperationSource source,
				final DomainModel afterModel)
		{
			var profile = policyBundle.profileResolver().resolve(source);
			var toleratedViolations = new ArrayList<OperationPolicyViolation>();
			var quarantiningViolations = new ArrayList<OperationPolicyViolation>();
			var rejectingViolations = new ArrayList<OperationPolicyViolation>();
			collect(
					policyBundle.accessPolicy().accessViolationFor(source, afterModel)
					            .map(OperationPolicyViolation::access),
					profile.accessViolationHandling(),
					toleratedViolations,
					quarantiningViolations,
					rejectingViolations);
			collect(
					policyBundle.creationPolicy().creationViolationFor(source, afterModel)
					            .map(OperationPolicyViolation::core),
					profile.coreViolationHandling(),
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
						Optional.of(OperationPolicyViolation.invariant(invariantViolation)),
						handling,
						toleratedViolations,
						quarantiningViolations,
						rejectingViolations);
			}
			collect(
					policyBundle.externalConsistencyPolicy().consistencyViolationFor(source, afterModel)
					            .map(OperationPolicyViolation::externalConsistency),
					profile.externalConsistencyViolationHandling(),
					toleratedViolations,
					quarantiningViolations,
					rejectingViolations);
			if (!quarantiningViolations.isEmpty())
			{
				return CreationPolicyDecision.quarantine(
						new CreationQuarantineRequest(source, quarantiningViolations),
						toleratedViolations);
			}
			if (!rejectingViolations.isEmpty())
			{
				throw rejectionFor(rejectingViolations.getFirst());
			}
			return CreationPolicyDecision.allow(toleratedViolations);
		}

		public void validate(final OperationSource source, final DomainModel afterModel)
		{
			var decision = evaluate(source, afterModel);
			if (decision.quarantined())
			{
				var request = decision.quarantineRequest().orElseThrow();
				log.warn("Creation quarantine stub engaged for source {} with violations {}", source,
						request.violations());
				throw QuarantinedCreationException.withRequest(request);
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
				case CORE, INVARIANT, EXTERNAL_CONSISTENCY -> InvalidRequestException.withMessage(violation.message());
			};
		}
	}
}