package de.gupta.clean.crud.template.useCases.query.specification.unique.api.web;

import de.gupta.clean.crud.template.useCases.query.specification.unique.api.behaviour.NotFoundStrategy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

public interface SpringRestUniqueSpecificationQueryController<APIModelResponse>
{
	@GetMapping("")
	ResponseEntity<Optional<APIModelResponse>> queryUniqueBy(
			@RequestParam(name = "notFoundStrategy", defaultValue = "THROW_EXCEPTION", required = false) NotFoundStrategy notFoundStrategy);
}