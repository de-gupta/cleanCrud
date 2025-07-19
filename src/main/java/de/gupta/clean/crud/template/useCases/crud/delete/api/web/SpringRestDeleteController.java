package de.gupta.clean.crud.template.useCases.crud.delete.api.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface SpringRestDeleteController<WebModelID>
{
	@DeleteMapping("/{id}")
	ResponseEntity<Void> deleteById(@PathVariable("id") @Valid final WebModelID id);
}