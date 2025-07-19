package de.gupta.clean.crud.template.useCases.crud.fetch.api.web;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.Set;

public interface SpringRestFetchController<WebModelResponse, WebModelID>
{
	@GetMapping("")
	ResponseEntity<Page<WebModelResponse>> findAll(Pageable pageable);

	@GetMapping("/{id}")
	ResponseEntity<WebModelResponse> findById(@PathVariable("id") @Valid final WebModelID id);

	@GetMapping("/ids")
	ResponseEntity<Map<WebModelID, WebModelResponse>> findAllByIds(
			@RequestParam("ids") @Valid final Set<WebModelID> ids);
}