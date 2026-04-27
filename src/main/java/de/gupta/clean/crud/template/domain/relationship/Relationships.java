package de.gupta.clean.crud.template.domain.relationship;

import java.util.Collection;
import java.util.Objects;

public interface Relationships
{
	Class<?> baseModelClass();

	Collection<Relationship> relationships();

	default Relationship relationship(final String propertyName)
	{
		return relationships().stream()
		                      .filter(relationship -> Objects.equals(relationship.propertyName(), propertyName))
		                      .findFirst()
		                      .orElseThrow(() -> new IllegalArgumentException(
									  "No relationship declared for property '%s'".formatted(propertyName)));
	}
}