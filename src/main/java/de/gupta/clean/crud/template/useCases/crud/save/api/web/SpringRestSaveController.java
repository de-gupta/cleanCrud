package de.gupta.clean.crud.template.useCases.crud.save.api.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;

public interface SpringRestSaveController<WebModelCreate, WebModelResponse>
{
	@PostMapping("")
	ResponseEntity<WebModelResponse> save(@RequestBody @Valid final WebModelCreate model);

	@PostMapping("/batch")
	ResponseEntity<Collection<WebModelResponse>> saveAll(@RequestBody @Valid final Collection<WebModelCreate> models);
}