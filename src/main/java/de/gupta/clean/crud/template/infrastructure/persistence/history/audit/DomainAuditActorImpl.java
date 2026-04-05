package de.gupta.clean.crud.template.infrastructure.persistence.history.audit;

import de.gupta.clean.crud.template.domain.model.builder.AbstractModelBuilder;

import java.util.Optional;

final class DomainAuditActorImpl implements DomainAuditActor
{
	private String actorId;
	private String displayName;
	private AuditActorKind actorKind;
	private AuthenticationKind authenticationKind;
	private String tokenId;
	private String sessionId;
	private String issuer;
	private String clientId;

	static DomainAuditActorBuilder builder()
	{
		return new Builder();
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

	private DomainAuditActorImpl()
	{
	}

	private static final class Builder extends AbstractModelBuilder<DomainAuditActor>
			implements DomainAuditActorBuilder
	{
		private final DomainAuditActorImpl model;

		@Override
		public Builder withActorId(final String actorId)
		{
			model.actorId = actorId;
			return this;
		}

		@Override
		public Builder withDisplayName(final Optional<String> displayName)
		{
			model.displayName = displayName.orElse(null);
			return this;
		}

		@Override
		public Builder withActorKind(final Optional<AuditActorKind> actorKind)
		{
			model.actorKind = actorKind.orElse(null);
			return this;
		}

		@Override
		public Builder withAuthenticationKind(final Optional<AuthenticationKind> authenticationKind)
		{
			model.authenticationKind = authenticationKind.orElse(null);
			return this;
		}

		@Override
		public Builder withTokenId(final Optional<String> tokenId)
		{
			model.tokenId = tokenId.orElse(null);
			return this;
		}

		@Override
		public Builder withSessionId(final Optional<String> sessionId)
		{
			model.sessionId = sessionId.orElse(null);
			return this;
		}

		@Override
		public Builder withIssuer(final Optional<String> issuer)
		{
			model.issuer = issuer.orElse(null);
			return this;
		}

		@Override
		public Builder withClientId(final Optional<String> clientId)
		{
			model.clientId = clientId.orElse(null);
			return this;
		}

		@Override
		protected DomainAuditActor doBuild()
		{
			return model;
		}

		private Builder()
		{
			super();
			this.model = new DomainAuditActorImpl();
		}
	}
}