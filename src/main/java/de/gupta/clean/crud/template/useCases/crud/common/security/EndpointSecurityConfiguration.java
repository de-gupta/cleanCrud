package de.gupta.clean.crud.template.useCases.crud.common.security;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EndpointSecurityConfiguration
{
	boolean enabled() default true;

	Class<? extends EndpointSecurityPolicy> endpointPolicy();
}