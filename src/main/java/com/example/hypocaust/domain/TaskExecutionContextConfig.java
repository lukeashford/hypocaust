package com.example.hypocaust.domain;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.db.ArtifactEntity.Kind;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Configuration record that groups all callbacks and function dependencies
 * required by TaskExecutionContext.
 *
 * <p>This record simplifies context initialization by bundling all the callbacks
 * that TaskService needs to provide to TaskExecutionContext.
 *
 * @param onArtifactAdded      Callback when a new artifact is scheduled
 * @param onArtifactUpdated    Callback when an artifact is updated
 * @param onArtifactRemoved    Callback when an artifact is removed/cancelled
 * @param onTaskProgressUpdated Callback when task progress tree changes
 * @param artifactExistsChecker Function to check if an artifact exists in predecessor state
 * @param artifactKindGetter    Function to get artifact kind from predecessor state
 * @param artifactNamesGetter   Function to get all artifact names from predecessor state
 * @param nameGenerator         Function to generate unique artifact names from descriptions
 * @param currentArtifactsGetter Function to get current artifacts from predecessor state
 */
public record TaskExecutionContextConfig(
    Consumer<TaskExecutionContext.ArtifactAddedEventData> onArtifactAdded,
    Consumer<TaskExecutionContext.ArtifactUpdatedEventData> onArtifactUpdated,
    Consumer<TaskExecutionContext.ArtifactRemovedEventData> onArtifactRemoved,
    Consumer<TaskTree> onTaskProgressUpdated,
    Function<String, Boolean> artifactExistsChecker,
    Function<String, Optional<Kind>> artifactKindGetter,
    Function<Void, Set<String>> artifactNamesGetter,
    Function<TaskExecutionContext.NameGenerationRequest, String> nameGenerator,
    Function<Void, List<ArtifactEntity>> currentArtifactsGetter
) {

  /**
   * Apply this configuration to a TaskExecutionContext.
   */
  public void applyTo(TaskExecutionContext context) {
    if (onArtifactAdded != null) {
      context.setOnArtifactAdded(onArtifactAdded);
    }
    if (onArtifactUpdated != null) {
      context.setOnArtifactUpdated(onArtifactUpdated);
    }
    if (onArtifactRemoved != null) {
      context.setOnArtifactRemoved(onArtifactRemoved);
    }
    if (onTaskProgressUpdated != null) {
      context.setOnTaskProgressUpdated(onTaskProgressUpdated);
    }
    if (artifactExistsChecker != null) {
      context.setArtifactExistsChecker(artifactExistsChecker);
    }
    if (artifactKindGetter != null) {
      context.setArtifactKindGetter(artifactKindGetter);
    }
    if (artifactNamesGetter != null) {
      context.setArtifactNamesGetter(artifactNamesGetter);
    }
    if (nameGenerator != null) {
      context.setNameGenerator(nameGenerator);
    }
    if (currentArtifactsGetter != null) {
      context.setCurrentArtifactsGetter(currentArtifactsGetter);
    }
  }

  /**
   * Builder for creating TaskExecutionContextConfig.
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Consumer<TaskExecutionContext.ArtifactAddedEventData> onArtifactAdded;
    private Consumer<TaskExecutionContext.ArtifactUpdatedEventData> onArtifactUpdated;
    private Consumer<TaskExecutionContext.ArtifactRemovedEventData> onArtifactRemoved;
    private Consumer<TaskTree> onTaskProgressUpdated;
    private Function<String, Boolean> artifactExistsChecker;
    private Function<String, Optional<Kind>> artifactKindGetter;
    private Function<Void, Set<String>> artifactNamesGetter;
    private Function<TaskExecutionContext.NameGenerationRequest, String> nameGenerator;
    private Function<Void, List<ArtifactEntity>> currentArtifactsGetter;

    public Builder onArtifactAdded(Consumer<TaskExecutionContext.ArtifactAddedEventData> callback) {
      this.onArtifactAdded = callback;
      return this;
    }

    public Builder onArtifactUpdated(Consumer<TaskExecutionContext.ArtifactUpdatedEventData> callback) {
      this.onArtifactUpdated = callback;
      return this;
    }

    public Builder onArtifactRemoved(Consumer<TaskExecutionContext.ArtifactRemovedEventData> callback) {
      this.onArtifactRemoved = callback;
      return this;
    }

    public Builder onTaskProgressUpdated(Consumer<TaskTree> callback) {
      this.onTaskProgressUpdated = callback;
      return this;
    }

    public Builder artifactExistsChecker(Function<String, Boolean> checker) {
      this.artifactExistsChecker = checker;
      return this;
    }

    public Builder artifactKindGetter(Function<String, Optional<Kind>> getter) {
      this.artifactKindGetter = getter;
      return this;
    }

    public Builder artifactNamesGetter(Function<Void, Set<String>> getter) {
      this.artifactNamesGetter = getter;
      return this;
    }

    public Builder nameGenerator(Function<TaskExecutionContext.NameGenerationRequest, String> generator) {
      this.nameGenerator = generator;
      return this;
    }

    public Builder currentArtifactsGetter(Function<Void, List<ArtifactEntity>> getter) {
      this.currentArtifactsGetter = getter;
      return this;
    }

    public TaskExecutionContextConfig build() {
      return new TaskExecutionContextConfig(
          onArtifactAdded,
          onArtifactUpdated,
          onArtifactRemoved,
          onTaskProgressUpdated,
          artifactExistsChecker,
          artifactKindGetter,
          artifactNamesGetter,
          nameGenerator,
          currentArtifactsGetter
      );
    }
  }
}
