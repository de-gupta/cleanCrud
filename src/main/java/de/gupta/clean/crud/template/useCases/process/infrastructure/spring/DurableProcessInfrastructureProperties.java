package de.gupta.clean.crud.template.useCases.process.infrastructure.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "clean-crud.process")
public class DurableProcessInfrastructureProperties
{
	private boolean enabled = true;
	private boolean pollingEnabled = true;
	private Duration pollInterval = Duration.ofMinutes(1);
	private int batchSize = 100;

	public boolean enabled()
	{
		return enabled;
	}

	public void setEnabled(final boolean enabled)
	{
		this.enabled = enabled;
	}

	public boolean pollingEnabled()
	{
		return pollingEnabled;
	}

	public void setPollingEnabled(final boolean pollingEnabled)
	{
		this.pollingEnabled = pollingEnabled;
	}

	public Duration pollInterval()
	{
		return pollInterval;
	}

	public void setPollInterval(final Duration pollInterval)
	{
		this.pollInterval = pollInterval;
	}

	public int batchSize()
	{
		return batchSize;
	}

	public void setBatchSize(final int batchSize)
	{
		this.batchSize = batchSize;
	}
}
