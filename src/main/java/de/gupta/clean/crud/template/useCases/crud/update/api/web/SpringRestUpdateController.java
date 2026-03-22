package de.gupta.clean.crud.template.useCases.crud.update.api.web;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;

public interface SpringRestUpdateController<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID>
{
	@PutMapping("/{id}")
	ResponseEntity<Void> putAtId(@PathVariable("id") @Valid final WebModelID id,
								 @RequestBody @Valid final WebModelCreate model);

	@PatchMapping("/{id}")
	ResponseEntity<WebModelResponse> updateById(@PathVariable("id") @Valid final WebModelID id,
												@RequestBody @Valid final WebModelUpdatePatch model);

	@PatchMapping("/batch")
	ResponseEntity<Collection<WebModelResponse>> updateAllById(
			@RequestBody @Valid final Collection<IdentifiedModel<WebModelID, WebModelUpdatePatch>> models);
}
