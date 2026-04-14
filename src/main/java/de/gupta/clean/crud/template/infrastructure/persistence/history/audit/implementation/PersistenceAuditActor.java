package de.gupta.clean.crud.template.infrastructure.persistence.history.audit.implementation;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.infrastructure.persistence.history.audit.AuditActor;
import de.gupta.clean.crud.template.infrastructure.persistence.history.audit.AuditActorKind;
import de.gupta.clean.crud.template.infrastructure.persistence.history.audit.AuthenticationKind;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;
import java.util.Optional;

@Embeddable
public class PersistenceAuditActor implements AuditActor
{
	private String actorId;
	private String displayName;

	@Enumerated(EnumType.STRING)
	private AuditActorKind actorKind;

	@Enumerated(EnumType.STRING)
	private AuthenticationKind authenticationKind;

	private String tokenId;
	private String sessionId;
	private String issuer;
	private String clientId;

	public static PersistenceAuditActor from(final AuditActor auditActor)
	{
		return Unfolding.beckon(auditActor)
		                .metamorphose(
								a ->
								{
									a.validate();
									return new PersistenceAuditActor(
											a.actorId(),
											a.displayName().orElse(null),
											a.actorKind().orElse(null),
											a.authenticationKind().orElse(null),
											a.tokenId().orElse(null),
											a.sessionId().orElse(null),
											a.issuer().orElse(null),
											a.clientId().orElse(null));
								})
		                .infuse(() -> null);
	}

	static PersistenceAuditActor of(final String actorId, final String displayName, final AuditActorKind actorKind,
	                                final AuthenticationKind authenticationKind, final String tokenId,
	                                final String sessionId, final String issuer, final String clientId)
	{
		return new PersistenceAuditActor(actorId, displayName, actorKind, authenticationKind, tokenId, sessionId,
				issuer,
				clientId);
	}

	@Override
	public String actorId()
	{
		return actorId;
	}

	@Override
	public Optional<String> displayName()
	{
		return Optional.ofNullable(displayName);
	}

	@Override
	public Optional<AuditActorKind> actorKind()
	{
		return Optional.ofNullable(actorKind);
	}

	@Override
	public Optional<AuthenticationKind> authenticationKind()
	{
		return Optional.ofNullable(authenticationKind);
	}

	@Override
	public Optional<String> tokenId()
	{
		return Optional.ofNullable(tokenId);
	}

	@Override
	public Optional<String> sessionId()
	{
		return Optional.ofNullable(sessionId);
	}

	@Override
	public Optional<String> issuer()
	{
		return Optional.ofNullable(issuer);
	}

	@Override
	public Optional<String> clientId()
	{
		return Optional.ofNullable(clientId);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(actorId, displayName, actorKind, authenticationKind, tokenId, sessionId, issuer, clientId);
	}

	@Override
	public boolean equals(final Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof PersistenceAuditActor auditActor))
		{
			return false;
		}
		return Objects.equals(actorId, auditActor.actorId) && Objects.equals(displayName,
				auditActor.displayName) && actorKind == auditActor.actorKind && authenticationKind == auditActor.authenticationKind && Objects.equals(
				tokenId, auditActor.tokenId) && Objects.equals(sessionId, auditActor.sessionId) && Objects.equals(
				issuer, auditActor.issuer) && Objects.equals(clientId, auditActor.clientId);
	}

	@Override
	public String toString()
	{
		return "PersistenceAuditActor{" + "actorId='" + actorId + '\'' + ", displayName='" + displayName + '\'' + ", actorKind=" + actorKind + ", authenticationKind=" + authenticationKind + ", tokenId='" + tokenId + '\'' + ", sessionId='" + sessionId + '\'' + ", issuer='" + issuer + '\'' + ", clientId='" + clientId + '\'' + '}';
	}

	PersistenceAuditActor(final String actorId, final String displayName, final AuditActorKind actorKind,
	                      final AuthenticationKind authenticationKind, final String tokenId,
	                      final String sessionId, final String issuer, final String clientId)
	{
		this.actorId = actorId;
		this.displayName = displayName;
		this.actorKind = actorKind;
		this.authenticationKind = authenticationKind;
		this.tokenId = tokenId;
		this.sessionId = sessionId;
		this.issuer = issuer;
		this.clientId = clientId;
		validate();
	}

	protected PersistenceAuditActor()
	{
	}
}