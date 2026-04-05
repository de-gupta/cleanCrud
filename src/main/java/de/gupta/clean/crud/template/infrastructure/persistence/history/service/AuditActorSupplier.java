package de.gupta.clean.crud.template.infrastructure.persistence.history.service;

import de.gupta.clean.crud.template.infrastructure.persistence.history.audit.AuditActor;

@FunctionalInterface
public interface AuditActorSupplier
{
	static AuditActorSupplier none()
	{
		return () -> null;
	}

	AuditActor get();
}