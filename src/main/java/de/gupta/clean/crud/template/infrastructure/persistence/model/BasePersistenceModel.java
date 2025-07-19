package de.gupta.clean.crud.template.infrastructure.persistence.model;

import de.gupta.clean.crud.template.domain.model.validation.Validatable;
import de.gupta.clean.crud.template.infrastructure.persistence.model.properties.WithID;

public interface BasePersistenceModel<ID> extends WithID<ID>, Validatable
{
}