package de.gupta.clean.crud.template.useCases.operation.quarantine.application.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

public final class DefaultQuarantineReplayRegistry<P> implements QuarantineReplayRegistry<P>
{
	private final java.util.Map<String, QuarantineReplayGateway<P>> gateways;

	public static <P> DefaultQuarantineReplayRegistry<P> of(
			final Collection<? extends QuarantineReplayGateway<P>> gateways)
	{
		return new DefaultQuarantineReplayRegistry<>(gateways);
	}

	@Override
	public Optional<QuarantineReplayGateway<P>> findGateway(final String aggregateKey)
	{
		return Optional.ofNullable(gateways.get(aggregateKey));
	}

	private DefaultQuarantineReplayRegistry(
			final Collection<? extends QuarantineReplayGateway<P>> gateways)
	{
		Objects.requireNonNull(gateways, "gateways");
		this.gateways = new LinkedHashMap<>();
		for (var gateway : gateways)
		{
			var duplicate = this.gateways.putIfAbsent(gateway.aggregateKey(), gateway);
			if (duplicate != null)
			{
				throw new IllegalArgumentException(
						"Duplicate quarantine replay gateway for aggregate key " + gateway.aggregateKey());
			}
		}
	}
}
