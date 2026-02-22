package com.example.hypocaust.domain.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import java.util.HashMap;
import java.util.Map;

class EventTypeIdResolver extends TypeIdResolverBase {

  private static final Map<EventType, Class<? extends Event<?>>> TYPE_TO_CLASS;

  static {
    Map<EventType, Class<? extends Event<?>>> map = new HashMap<>();
    map.put(EventType.ARTIFACT_ADDED, ArtifactAddedEvent.class);
    map.put(EventType.ARTIFACT_UPDATED, ArtifactUpdatedEvent.class);
    map.put(EventType.ARTIFACT_REMOVED, ArtifactRemovedEvent.class);
    map.put(EventType.TASKEXECUTION_STARTED, TaskExecutionStartedEvent.class);
    map.put(EventType.TASKEXECUTION_COMPLETED, TaskExecutionCompletedEvent.class);
    map.put(EventType.TASKEXECUTION_FAILED, TaskExecutionFailedEvent.class);
    map.put(EventType.TODO_LIST_UPDATED, TodoListUpdatedEvent.class);
    map.put(EventType.TOOL_CALLING, ToolCallingEvent.class);
    map.put(EventType.ERROR, ErrorEvent.class);
    map.put(EventType.DECOMPOSER_STARTED, DecomposerStartedEvent.class);
    map.put(EventType.DECOMPOSER_FINISHED, DecomposerFinishedEvent.class);
    map.put(EventType.DECOMPOSER_FAILED, DecomposerFailedEvent.class);
    TYPE_TO_CLASS = Map.copyOf(map);
  }

  private static final Map<Class<?>, EventType> CLASS_TO_TYPE =
      TYPE_TO_CLASS.entrySet().stream()
          .collect(java.util.stream.Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

  @Override
  public String idFromValue(Object value) {
    return CLASS_TO_TYPE.get(value.getClass()).getValue();
  }

  @Override
  public String idFromValueAndType(Object value, Class<?> suggestedType) {
    return idFromValue(value);
  }

  @Override
  public JavaType typeFromId(DatabindContext context, String id) {
    EventType eventType = EventType.fromValue(id);
    Class<? extends Event<?>> eventClass = TYPE_TO_CLASS.get(eventType);
    return context.constructType(eventClass);
  }

  @Override
  public JsonTypeInfo.Id getMechanism() {
    return JsonTypeInfo.Id.CUSTOM;
  }
}
