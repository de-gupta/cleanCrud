package de.gupta.clean.crud.template.useCases.operation.mutation.quarantine.application;

import java.util.*;

public final class DefaultMutationQuarantineReplayRegistry implements MutationQuarantineReplayRegistry
{
	private final Map<String, MutationQuarantineReplayGateway> gateways;

	public static DefaultMutationQuarantineReplayRegistry of(
			final Collection<? extends MutationQuarantineReplayGateway> gateways)
	{
		return new DefaultMutationQuarantineReplayRegistry(gateways);
	}

	@Override
	public Optional<MutationQuarantineReplayGateway> findGateway(final String aggregateType)
	{
		return Optional.ofNullable(gateways.get(aggregateType));
	}

	private DefaultMutationQuarantineReplayRegistry(
			final Collection<? extends MutationQuarantineReplayGateway> gateways)
	{
		Objects.requireNonNull(gateways, "gateways");
		this.gateways = new LinkedHashMap<>();
		for (var gateway : gateways)
		{
			var duplicate = this.gateways.putIfAbsent(gateway.aggregateType(), gateway);
			if (duplicate != null)
			{
				throw new IllegalArgumentException(
						"Duplicate mutation quarantine replay gateway for aggregate type "
								+ gateway.aggregateType());
			}
		}
	}
}