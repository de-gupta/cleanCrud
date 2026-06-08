package de.gupta.clean.crud.template.useCases.process.application.registration;

import java.util.Collection;
import java.util.Optional;

public interface DurableProcessDefinitionRegistry
{
	Optional<DurableRegisteredProcess<?, ?>> registeredProcess(String processType);

	Collection<DurableRegisteredProcess<?, ?>> registeredProcesses();
}
