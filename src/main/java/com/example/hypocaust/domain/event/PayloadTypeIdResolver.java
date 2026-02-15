package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.event.ErrorEvent.ErrorEventPayload;
import com.example.hypocaust.domain.event.Event.EventPayload;
import com.example.hypocaust.domain.event.ToolCallingEvent.ToolCallingEventPayload;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class PayloadTypeIdResolver extends TypeIdResolverBase {

  private static final Map<EventType, Class<? extends EventPayload>> TYPE_TO_CLASS;

  static {
    Map<EventType, Class<? extends EventPayload>> map = new HashMap<>();
    map.put(EventType.ARTIFACT_ADDED, Artifact.class);
    map.put(EventType.ARTIFACT_UPDATED, Artifact.class);
    map.put(EventType.ARTIFACT_REMOVED, ArtifactRemovedEvent.Payload.class);
    map.put(EventType.TASKEXECUTION_STARTED, TaskExecutionStartedEvent.Payload.class);
    map.put(EventType.TASKEXECUTION_COMPLETED, TaskExecutionCompletedEvent.Payload.class);
    map.put(EventType.TASKEXECUTION_FAILED, TaskExecutionFailedEvent.Payload.class);
    map.put(EventType.TASK_PROGRESS_UPDATED, TodoListUpdatedEvent.Payload.class);
    map.put(EventType.TOOL_CALLING, ToolCallingEventPayload.class);
    map.put(EventType.ERROR, ErrorEventPayload.class);
    map.put(EventType.DECOMPOSER_STARTED, DecomposerStartedEvent.Payload.class);
    map.put(EventType.DECOMPOSER_FINISHED, DecomposerFinishedEvent.Payload.class);
    map.put(EventType.DECOMPOSER_FAILED, DecomposerFailedEvent.Payload.class);
    TYPE_TO_CLASS = Map.copyOf(map);
  }

  private static final Map<Class<?>, EventType> CLASS_TO_TYPE =
      TYPE_TO_CLASS.entrySet().stream()
          .collect(Collectors.toMap(
              Map.Entry::getValue,
              Map.Entry::getKey,
              (existing, replacement) -> existing // Keep the first one in case of collision
          ));

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
    Class<? extends EventPayload> payloadClass = TYPE_TO_CLASS.get(eventType);
    return context.constructType(payloadClass);
  }

  @Override
  public JsonTypeInfo.Id getMechanism() {
    return JsonTypeInfo.Id.CUSTOM;
  }
}
