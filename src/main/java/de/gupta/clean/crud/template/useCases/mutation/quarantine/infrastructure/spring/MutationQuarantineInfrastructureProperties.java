package de.gupta.clean.crud.template.useCases.mutation.quarantine.infrastructure.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clean-crud.mutation.quarantine")
public record MutationQuarantineInfrastructureProperties(
		boolean enabled,
		boolean apiEnabled)
{
	public MutationQuarantineInfrastructureProperties()
	{
		this(true, false);
	}
}
