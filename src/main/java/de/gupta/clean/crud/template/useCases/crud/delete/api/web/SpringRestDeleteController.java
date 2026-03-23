package de.gupta.clean.crud.template.useCases.crud.delete.api.web;

import de.gupta.clean.crud.template.useCases.crud.common.BulkOperationMode;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

public interface SpringRestDeleteController<WebModelID>
{
	@DeleteMapping("/{id}")
	ResponseEntity<Void> deleteById(@PathVariable("id") @Valid final WebModelID id);

	@DeleteMapping("/batch")
	ResponseEntity<Void> deleteAllById(
			@RequestBody @Valid final Collection<WebModelID> ids,
			@RequestParam(name = "mode", defaultValue = "ALL_OR_NOTHING") final BulkOperationMode mode);
}