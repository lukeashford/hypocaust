package com.example.hypocaust.models;

import java.util.EnumMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExecutionRouter {

  private final EnumMap<Platform, ModelExecutor> executors;

  public ExecutionRouter(List<ModelExecutor> executorList) {
    this.executors = new EnumMap<>(Platform.class);
    for (var executor : executorList) {
      executors.put(executor.platform(), executor);
    }
  }

  public ModelExecutor resolve(Platform platform) {
    var executor = executors.get(platform);
    if (executor == null) {
      throw new IllegalArgumentException("No executor registered for platform: " + platform);
    }
    return executor;
  }

  public ModelExecutor resolve(String platformName) {
    return resolve(Platform.valueOf(platformName));
  }
}
