package de.gupta.clean.crud.template.useCases.query.specification.collection.api.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collection;

public interface SpringRestSpecificationQueryController<APIModelResponse>
{
	@GetMapping("")
	ResponseEntity<Collection<APIModelResponse>> queryBy();
}