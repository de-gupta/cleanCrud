package de.gupta.clean.crud.template.infrastructure.persistence.history.audit;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.clean.crud.template.domain.model.builder.ModelBuilder;
import de.gupta.clean.crud.template.domain.model.exceptions.validation.RequiredFieldNotSetException;
import de.gupta.clean.crud.template.domain.model.validation.Validatable;
import de.gupta.commons.utility.string.StringSanitizationUtility;

import java.util.Optional;

public interface AuditActor extends Validatable
{
	String actorId();

	Optional<String> displayName();

	Optional<AuditActorKind> actorKind();

	Optional<AuthenticationKind> authenticationKind();

	Optional<String> tokenId();

	Optional<String> sessionId();

	Optional<String> issuer();

	Optional<String> clientId();

	@Override
	default void validate()
	{
		Unfolding.beckon(actorId())
		         .discern(StringSanitizationUtility::isNotBlank, RequiredFieldNotSetException.forMessage(
						 "auditActor.actorId must be provided when an audit actor exists"));
	}

	interface AuditActorBuilder<M extends AuditActor, B extends AuditActorBuilder<M, B>> extends ModelBuilder<M>
	{
		B withActorId(String actorId);

		B withDisplayName(Optional<String> displayName);

		B withActorKind(Optional<AuditActorKind> actorKind);

		B withAuthenticationKind(Optional<AuthenticationKind> authenticationKind);

		B withTokenId(Optional<String> tokenId);

		B withSessionId(Optional<String> sessionId);

		B withIssuer(Optional<String> issuer);

		B withClientId(Optional<String> clientId);
	}
}