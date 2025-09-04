package com.example.the_machine.models;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ModelPlatform {

  public abstract String getName();

  protected abstract List<ModelBuilder> getAllBuilders();

  public abstract GenericModelBuilder<?, ?> getGenericBuilder();

  /**
   * Gets a specific model builder by name. Throws exception if not found - explicit behavior.
   */
  public ModelBuilder getBuilder(String modelName) {
    return getAllBuilders().stream()
        .filter(builder -> builder.getName().equals(modelName))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No builder found for model '" + modelName + "' on platform: " + getName()));
  }

  /**
   * Always returns the default builder - explicit fallback.
   */
  public ModelBuilder getDefaultBuilder() {
    return getAllBuilders().stream()
        .filter(builder -> "default".equals(builder.getName()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No default builder available on platform: " + getName()));
  }

  /**
   * Returns builder or default with warning - convenience method.
   */
  public ModelBuilder getBuilderOrDefault(String modelName) {
    try {
      return getBuilder(modelName);
    } catch (IllegalStateException e) {
      log.warn("Model builder '{}' not found on platform '{}', using default",
          modelName, getName());
      return getDefaultBuilder();
    }
  }
}
