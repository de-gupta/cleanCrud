package de.gupta.clean.crud.template.infrastructure.persistence.history.model;

import de.gupta.aletheia.functional.Unfolding;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;

@Embeddable
class PersistedAuditActor implements AuditActor
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

	static PersistedAuditActor from(final AuditActor auditActor)
	{
		return Unfolding.beckon(auditActor)
		                .metamorphose(
								a ->
								{
									a.validate();
									return new PersistedAuditActor(a.actorId(), a.displayName(), a.actorKind(),
											a.authenticationKind(),
											a.tokenId(), a.sessionId(), a.issuer(), a.clientId());
								})
		                .rescue(() -> null);
	}

	static PersistedAuditActor of(final String actorId, final String displayName, final AuditActorKind actorKind,
	                              final AuthenticationKind authenticationKind, final String tokenId,
	                              final String sessionId, final String issuer, final String clientId)
	{
		return new PersistedAuditActor(actorId, displayName, actorKind, authenticationKind, tokenId, sessionId, issuer,
				clientId);
	}

	@Override
	public String actorId()
	{
		return actorId;
	}

	@Override
	public String displayName()
	{
		return displayName;
	}

	@Override
	public AuditActorKind actorKind()
	{
		return actorKind;
	}

	@Override
	public AuthenticationKind authenticationKind()
	{
		return authenticationKind;
	}

	@Override
	public String tokenId()
	{
		return tokenId;
	}

	@Override
	public String sessionId()
	{
		return sessionId;
	}

	@Override
	public String issuer()
	{
		return issuer;
	}

	@Override
	public String clientId()
	{
		return clientId;
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
		if (!(other instanceof PersistedAuditActor auditActor))
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
		return "PersistedAuditActor{" + "actorId='" + actorId + '\'' + ", displayName='" + displayName + '\'' + ", actorKind=" + actorKind + ", authenticationKind=" + authenticationKind + ", tokenId='" + tokenId + '\'' + ", sessionId='" + sessionId + '\'' + ", issuer='" + issuer + '\'' + ", clientId='" + clientId + '\'' + '}';
	}

	PersistedAuditActor(final String actorId, final String displayName, final AuditActorKind actorKind,
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

	protected PersistedAuditActor()
	{
	}
}