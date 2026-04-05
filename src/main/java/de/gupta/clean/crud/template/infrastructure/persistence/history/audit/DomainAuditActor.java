package de.gupta.clean.crud.template.infrastructure.persistence.history.audit;

public sealed interface DomainAuditActor extends AuditActor permits DomainAuditActorImpl
{
	interface DomainAuditActorBuilder extends AuditActor.AuditActorBuilder<DomainAuditActor, DomainAuditActorBuilder>
	{
	}
}