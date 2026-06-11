package de.gupta.clean.crud.template.useCases.operation.quarantine.infrastructure.persistence.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "creation_quarantine")
public class CreationQuarantineEntity extends QuarantinePersistenceModel
{
}
