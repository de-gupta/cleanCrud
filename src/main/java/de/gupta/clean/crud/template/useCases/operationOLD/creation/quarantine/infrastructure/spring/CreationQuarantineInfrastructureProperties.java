package de.gupta.clean.crud.template.useCases.operationOLD.creation.quarantine.infrastructure.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clean-crud.creation.quarantine")
public record CreationQuarantineInfrastructureProperties(
		boolean enabled,
		boolean apiEnabled)
{
	public CreationQuarantineInfrastructureProperties()
	{
		this(true, false);
	}
}