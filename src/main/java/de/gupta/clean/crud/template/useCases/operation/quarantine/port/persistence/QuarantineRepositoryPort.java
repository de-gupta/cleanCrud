package de.gupta.clean.crud.template.useCases.operation.quarantine.port.persistence;

import java.util.Collection;
import java.util.Optional;

public interface QuarantineRepositoryPort<Id, Record>
{
	Record save(Record record);

	Record update(Record record);

	Optional<Record> findById(Id id);

	Collection<Record> findOpen(int limit);
}
