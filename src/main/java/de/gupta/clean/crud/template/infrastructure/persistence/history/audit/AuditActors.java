package de.gupta.clean.crud.template.infrastructure.persistence.history.audit;

public final class AuditActors
{
	public static AuditActor.AuditActorBuilder<DomainAuditActor, ?> builder()
	{
		return DomainAuditActorImpl.builder();
	}

	public static AuditActor withId(final String id)
	{
		return builder().withActorId(id).build();
	}

	private AuditActors()
	{
	}
}