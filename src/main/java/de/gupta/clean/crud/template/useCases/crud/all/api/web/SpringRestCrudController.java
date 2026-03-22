package de.gupta.clean.crud.template.useCases.crud.all.api.web;

import de.gupta.clean.crud.template.domain.model.identified.IdentifiedModel;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Deprecated
public interface SpringRestCrudController<WebModelCreate, WebModelUpdatePatch, WebModelResponse, WebModelID>
{
	@GetMapping("")
	ResponseEntity<Page<WebModelResponse>> findAll(Pageable pageable);

	@GetMapping("/{id}")
	ResponseEntity<WebModelResponse> findById(@PathVariable("id") @Valid final WebModelID id);

	@PostMapping("")
	ResponseEntity<WebModelResponse> save(@RequestBody @Valid final WebModelCreate model);

	@PostMapping("/batch")
	ResponseEntity<Collection<WebModelResponse>> saveAll(@RequestBody @Valid final Collection<WebModelCreate> models);

	@PutMapping("/{id}")
	ResponseEntity<Void> putAtId(@PathVariable("id") @Valid final WebModelID id,
								 @RequestBody @Valid final WebModelCreate model);

	@PatchMapping("/{id}")
	ResponseEntity<WebModelResponse> updateById(@PathVariable("id") @Valid final WebModelID id,
												@RequestBody @Valid final WebModelUpdatePatch model);

	@PatchMapping("/batch")
	ResponseEntity<Collection<WebModelResponse>> updateAllById(
			@RequestBody @Valid final Collection<IdentifiedModel<WebModelID, WebModelUpdatePatch>> models);

	@DeleteMapping("/{id}")
	ResponseEntity<Void> deleteById(@PathVariable("id") @Valid final WebModelID id);

	@DeleteMapping("/batch")
	ResponseEntity<Void> deleteAllById(@RequestBody @Valid final Collection<WebModelID> ids);
}
