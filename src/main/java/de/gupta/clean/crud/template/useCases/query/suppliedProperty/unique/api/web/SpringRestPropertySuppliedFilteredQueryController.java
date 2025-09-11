package de.gupta.clean.crud.template.useCases.query.suppliedProperty.unique.api.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

public interface SpringRestPropertySuppliedFilteredQueryController<APIModelResponse>
{
	@GetMapping("")
	ResponseEntity<APIModelResponse> queryBySuppliedPropertyAndFilter();
}