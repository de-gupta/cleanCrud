package de.gupta.clean.crud.template.useCases.operationOLD.mutation.domain.plan;

import de.gupta.clean.crud.template.domain.aggregate.intent.SatelliteMutationIntent;

import java.util.*;

public record AggregateMutationPlan<DomainModel>(
		Optional<DomainModel> updatedRoot,
		Map<String, Collection<SatelliteMutationIntent<?, ?, ?>>> relationshipMutations)
{
	public static <DomainModel> AggregateMutationPlan<DomainModel> rootOnly(final DomainModel updatedRoot)
	{
		return builder(updatedRoot).build();
	}

	public static <DomainModel> Builder<DomainModel> builder(final DomainModel updatedRoot)
	{
		return new Builder<DomainModel>().updatedRoot(updatedRoot);
	}

	public static <DomainModel> Builder<DomainModel> builder()
	{
		return new Builder<>();
	}

	public AggregateMutationPlan
	{
		Objects.requireNonNull(updatedRoot, "updatedRoot");
		Objects.requireNonNull(relationshipMutations, "relationshipMutations");
		relationshipMutations = relationshipMutations.entrySet()
		                                             .stream()
		                                             .collect(LinkedHashMap::new,
															 (map, entry) -> map.put(
																	 Objects.requireNonNull(entry.getKey(),
																			 "relationshipName"),
																	 List.copyOf(Objects.requireNonNull(
																			 entry.getValue(),
																			 "relationshipMutations"))),
															 LinkedHashMap::putAll);
	}

	public boolean hasRelationshipMutations()
	{
		return !relationshipMutations.isEmpty();
	}

	public static final class Builder<DomainModel>
	{
		private final Map<String, Collection<SatelliteMutationIntent<?, ?, ?>>> relationshipMutations =
				new LinkedHashMap<>();
		private DomainModel updatedRoot;

		public Builder<DomainModel> updatedRoot(final DomainModel updatedRoot)
		{
			this.updatedRoot = Objects.requireNonNull(updatedRoot, "updatedRoot");
			return this;
		}

		public Builder<DomainModel> mutateRelationship(
				final String relationshipName,
				final Collection<? extends SatelliteMutationIntent<?, ?, ?>> mutationIntents)
		{
			relationshipMutations.put(
					Objects.requireNonNull(relationshipName, "relationshipName"),
					List.copyOf(Objects.requireNonNull(mutationIntents, "mutationIntents")));
			return this;
		}

		public AggregateMutationPlan<DomainModel> build()
		{
			return new AggregateMutationPlan<>(Optional.ofNullable(updatedRoot), relationshipMutations);
		}

		private Builder()
		{
		}
	}
}