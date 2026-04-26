package de.gupta.clean.crud.template.domain.relationship;

import java.util.Collection;

public interface Relationships
{
	Class<?> baseModelClass();

	Collection<Relationship> relationships();
}