package de.gupta.clean.crud.template.useCases.crud.delete.api.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;

public interface SpringRestDeleteController<WebModelID>
{
	@DeleteMapping("/{id}")
	ResponseEntity<Void> deleteById(@PathVariable @Valid final WebModelID id);

	@DeleteMapping("/batch")
	ResponseEntity<Void> deleteAllById(@RequestBody @Valid final Collection<WebModelID> ids);
}