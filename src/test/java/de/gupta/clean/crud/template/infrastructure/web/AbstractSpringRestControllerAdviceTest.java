package de.gupta.clean.crud.template.infrastructure.web;

import de.gupta.clean.crud.template.domain.model.exceptions.resource.ResourceStateConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractSpringRestControllerAdviceTest
{
	@Test
	void handleResourceStateConflictExceptionReturnsConflict()
	{
		AbstractSpringRestControllerAdvice advice = new AbstractSpringRestControllerAdvice();
		ResourceStateConflictException exception =
				ResourceStateConflictException.withMessage("Multiple current history records found.");

		var response = advice.handleResourceStateConflictException(exception);

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertEquals("Multiple current history records found.", response.getBody());
	}
}
