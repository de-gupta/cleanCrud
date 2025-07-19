package de.gupta.clean.crud.template.domain.model.builder;

public interface ModelBuilderFactory<Model, V extends ModelBuilder<? extends Model>>
{
	V builder();
}