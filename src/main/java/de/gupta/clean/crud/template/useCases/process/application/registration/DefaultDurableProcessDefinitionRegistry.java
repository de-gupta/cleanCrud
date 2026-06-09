package de.gupta.clean.crud.template.useCases.process.application.registration;

import java.util.*;

public final class DefaultDurableProcessDefinitionRegistry implements DurableProcessDefinitionRegistry
{
	private final Map<String, DurableRegisteredProcess<?, ?>> registeredProcesses;

	public static DefaultDurableProcessDefinitionRegistry of(
			final Collection<? extends DurableRegisteredProcess<?, ?>> registeredProcesses)
	{
		return new DefaultDurableProcessDefinitionRegistry(registeredProcesses);
	}

	@Override
	public Optional<DurableRegisteredProcess<?, ?>> registeredProcess(final String processType)
	{
		return Optional.ofNullable(registeredProcesses.get(processType));
	}

	@Override
	public Collection<DurableRegisteredProcess<?, ?>> registeredProcesses()
	{
		return List.copyOf(registeredProcesses.values());
	}

	private DefaultDurableProcessDefinitionRegistry(
			final Collection<? extends DurableRegisteredProcess<?, ?>> registeredProcesses)
	{
		Objects.requireNonNull(registeredProcesses, "registeredProcesses");
		this.registeredProcesses = new LinkedHashMap<>();
		for (var registeredProcess : registeredProcesses)
		{
			var previous = this.registeredProcesses.putIfAbsent(
					registeredProcess.definition().processType(),
					Objects.requireNonNull(registeredProcess, "registeredProcess"));
			if (previous != null)
			{
				throw new IllegalArgumentException(
						"Duplicate process type: " + registeredProcess.definition().processType());
			}
		}
	}
}