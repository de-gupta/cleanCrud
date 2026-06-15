package de.gupta.clean.crud.template.domain.aggregate.relationship.standard;

import de.gupta.clean.crud.template.domain.aggregate.definition.AggregateDefinition;
import de.gupta.clean.crud.template.domain.aggregate.relationship.AggregateRelationshipDefinition;
import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import de.gupta.clean.crud.template.domain.relationship.Relationship;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class RelationshipDrivenAggregateRelationshipBuilder<
		MasterDomainId,
		MasterDomainModel,
		MasterDomainModelCreate,
		MasterDomainModelUpdatePatch,
		SatelliteDomainId,
		SatelliteDomainModel,
		SatelliteDomainModelCreate,
		SatelliteDomainModelUpdatePatch>
{
	private final Relationship relationship;
	private final AggregateDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
			SatelliteDomainModelUpdatePatch, ?> satelliteDefinition;

	public RelationshipDrivenAggregateRelationshipBuilder(
			final Relationship relationship,
			final AggregateDefinition<SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
					SatelliteDomainModelUpdatePatch, ?> satelliteDefinition)
	{
		this.relationship = Objects.requireNonNull(relationship, "relationship");
		this.satelliteDefinition = Objects.requireNonNull(satelliteDefinition, "satelliteDefinition");
	}

	public OneBindingBuilder current(
			final Function<MasterDomainModel, Optional<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>>
					current)
	{
		return new OneBindingBuilder(current);
	}

	public ManyBindingBuilder currentMany(
			final Function<MasterDomainModel, Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>>
					current)
	{
		return new ManyBindingBuilder(current);
	}

	public final class OneBindingBuilder
	{
		private final Function<MasterDomainModel, Optional<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>>
				current;
		private BiFunction<MasterDomainModel, Optional<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>,
				MasterDomainModel> replace;

		public OneBindingBuilder replace(
				final BiFunction<MasterDomainModel,
						Optional<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>, MasterDomainModel>
						replace)
		{
			this.replace = Objects.requireNonNull(replace, "replace");
			return this;
		}

		public AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> build()
		{
			return StandardRelationshipLoweringSupport.buildOne(
					relationship,
					satelliteDefinition,
					current,
					Objects.requireNonNull(replace, "replace"));
		}

		private OneBindingBuilder(
				final Function<MasterDomainModel, Optional<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>>
						current)
		{
			this.current = Objects.requireNonNull(current, "current");
		}
	}

	public final class ManyBindingBuilder
	{
		private final Function<MasterDomainModel, Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>>
				current;
		private BiFunction<MasterDomainModel, Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>,
				MasterDomainModel> replaceMany;

		public ManyBindingBuilder replaceMany(
				final BiFunction<MasterDomainModel,
						Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>, MasterDomainModel>
						replaceMany)
		{
			this.replaceMany = Objects.requireNonNull(replaceMany, "replaceMany");
			return this;
		}

		public AggregateRelationshipDefinition<MasterDomainId, MasterDomainModel, MasterDomainModelCreate,
				MasterDomainModelUpdatePatch, SatelliteDomainId, SatelliteDomainModel, SatelliteDomainModelCreate,
				SatelliteDomainModelUpdatePatch> build()
		{
			return StandardRelationshipLoweringSupport.buildMany(
					relationship,
					satelliteDefinition,
					current,
					Objects.requireNonNull(replaceMany, "replaceMany"));
		}

		private ManyBindingBuilder(
				final Function<MasterDomainModel, Collection<IdentifiedModel<SatelliteDomainId, SatelliteDomainModel>>>
						current)
		{
			this.current = Objects.requireNonNull(current, "current");
		}
	}
}