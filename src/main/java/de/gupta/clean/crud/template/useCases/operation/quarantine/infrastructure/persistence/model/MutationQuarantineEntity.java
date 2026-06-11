package de.gupta.clean.crud.template.useCases.operation.quarantine.infrastructure.persistence.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "mutation_quarantine")
public class MutationQuarantineEntity extends QuarantinePersistenceModel
{
}
