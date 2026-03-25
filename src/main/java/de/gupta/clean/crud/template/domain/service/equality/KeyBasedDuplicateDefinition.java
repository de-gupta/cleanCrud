package de.gupta.clean.crud.template.domain.service.equality;

import java.util.Objects;

@FunctionalInterface
public interface KeyBasedDuplicateDefinition<DomainModel, Key> extends DuplicateDefinition<DomainModel>
{
	Key duplicateKeyOf(DomainModel model);

	@Override
	default boolean areDuplicates(final DomainModel left, final DomainModel right)
	{
		return Objects.equals(duplicateKeyOf(left), duplicateKeyOf(right));
	}
}
