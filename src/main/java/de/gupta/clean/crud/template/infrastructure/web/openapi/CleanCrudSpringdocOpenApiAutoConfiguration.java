package de.gupta.clean.crud.template.infrastructure.web.openapi;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

import java.util.Arrays;
import java.util.Locale;

@AutoConfiguration
@ConditionalOnClass(name = {
		"org.springdoc.core.customizers.GlobalOpenApiCustomizer",
		"io.swagger.v3.oas.models.OpenAPI"
})
public class CleanCrudSpringdocOpenApiAutoConfiguration
{
	@Bean
	@ConditionalOnMissingBean(name = "cleanCrudOperationIdCustomizer")
	GlobalOpenApiCustomizer cleanCrudOperationIdCustomizer()
	{
		return openAPI ->
		{
			if (openAPI.getPaths() == null)
			{
				return;
			}

			openAPI.getPaths().forEach((path, pathItem) ->
			{
				if (pathItem == null)
				{
					return;
				}

				pathItem.readOperationsMap().forEach((httpMethod, operation) ->
						applyOperationId(path, httpMethod, operation));
			});
		};
	}

	private void applyOperationId(
			final String path,
			final PathItem.HttpMethod httpMethod,
			final Operation operation)
	{
		if (operation == null)
		{
			return;
		}

		operation.setOperationId(operationIdFrom(path, httpMethod));
	}

	private String operationIdFrom(final String path, final PathItem.HttpMethod httpMethod)
	{
		final var normalizedPath = normalizePath(path, httpMethod);
		if (!normalizedPath.isBlank())
		{
			return normalizedPath;
		}

		return httpMethod.name().toLowerCase(Locale.ROOT);
	}

	private String normalizePath(final String path, final PathItem.HttpMethod httpMethod)
	{
		final String normalized = Arrays.stream(path.split("/"))
		                                .map(String::trim)
		                                .filter(segment -> !segment.isBlank())
		                                .map(segment -> segment.replace("{", "").replace("}", ""))
		                                .map(segment -> segment.replace('-', '.').replace('_', '.'))
		                                .map(segment -> segment.replaceAll("\\.+", "."))
		                                .map(segment -> segment.replaceAll("(^\\.|\\.$)", ""))
		                                .filter(segment -> !segment.isBlank())
		                                .reduce((left, right) -> left + "." + right)
		                                .orElse("");

		if (httpMethod == PathItem.HttpMethod.PUT)
		{
			return normalized.replace(".update.", ".replace.")
			                 .replaceAll("\\.update$", ".replace");
		}

		return normalized;
	}

	@Configuration
	@Profile("swagger")
	@PropertySource(value = "classpath:cleancrud-swagger-defaults.properties", ignoreResourceNotFound = true)
	static class SwaggerProfileDefaultsConfiguration
	{
	}
}
