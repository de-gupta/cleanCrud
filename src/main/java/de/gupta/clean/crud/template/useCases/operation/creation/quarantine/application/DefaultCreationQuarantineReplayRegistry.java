package de.gupta.clean.crud.template.useCases.operation.creation.quarantine.application;

import java.util.*;

public final class DefaultCreationQuarantineReplayRegistry implements CreationQuarantineReplayRegistry
{
	private final Map<String, CreationQuarantineReplayGateway> gateways;

	public static DefaultCreationQuarantineReplayRegistry of(
			final Collection<? extends CreationQuarantineReplayGateway> gateways)
	{
		return new DefaultCreationQuarantineReplayRegistry(gateways);
	}

	@Override
	public Optional<CreationQuarantineReplayGateway> findGateway(final String aggregateType)
	{
		return Optional.ofNullable(gateways.get(aggregateType));
	}

	private DefaultCreationQuarantineReplayRegistry(
			final Collection<? extends CreationQuarantineReplayGateway> gateways)
	{
		Objects.requireNonNull(gateways, "gateways");
		this.gateways = new LinkedHashMap<>();
		for (var gateway : gateways)
		{
			var duplicate = this.gateways.putIfAbsent(gateway.aggregateType(), gateway);
			if (duplicate != null)
			{
				throw new IllegalArgumentException(
						"Duplicate creation quarantine replay gateway for aggregate type "
								+ gateway.aggregateType());
			}
		}
	}
}