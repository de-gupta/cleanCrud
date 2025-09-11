package de.gupta.clean.crud.template.useCases.query.specification.unique.api.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

public interface SpringRestUniqueSpecificationQueryController<APIModelResponse>
{
	@GetMapping("")
	ResponseEntity<Optional<APIModelResponse>> queryUniqueBy();
}