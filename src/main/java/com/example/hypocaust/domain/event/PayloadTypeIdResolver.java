package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.event.ErrorEvent.ErrorEventPayload;
import com.example.hypocaust.domain.event.Event.EventPayload;
import com.example.hypocaust.domain.event.ToolCallingEvent.ToolCallingEventPayload;
import com.example.hypocaust.dto.ArtifactDto;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import java.util.HashMap;
import java.util.Map;

class PayloadTypeIdResolver extends TypeIdResolverBase {

  /**
   * Maps EventType → Java class for deserialization (DB reads). Artifact events always deserialize
   * to {@link Artifact} — the internal representation that carries storageKey.
   */
  private static final Map<EventType, Class<? extends EventPayload>> TYPE_TO_CLASS;

  static {
    Map<EventType, Class<? extends EventPayload>> map = new HashMap<>();
    map.put(EventType.ARTIFACT_ADDED, Artifact.class);
    map.put(EventType.ARTIFACT_UPDATED, Artifact.class);
    map.put(EventType.ARTIFACT_REMOVED, ArtifactRemovedEvent.Payload.class);
    map.put(EventType.TASKEXECUTION_STARTED, TaskExecutionStartedEvent.Payload.class);
    map.put(EventType.TASKEXECUTION_COMPLETED, TaskExecutionCompletedEvent.Payload.class);
    map.put(EventType.TASKEXECUTION_FAILED, TaskExecutionFailedEvent.Payload.class);
    map.put(EventType.TODO_LIST_UPDATED, TodoListUpdatedEvent.Payload.class);
    map.put(EventType.TOOL_CALLING, ToolCallingEventPayload.class);
    map.put(EventType.ERROR, ErrorEventPayload.class);
    map.put(EventType.DECOMPOSER_STARTED, DecomposerStartedEvent.Payload.class);
    map.put(EventType.DECOMPOSER_FINISHED, DecomposerFinishedEvent.Payload.class);
    map.put(EventType.DECOMPOSER_FAILED, DecomposerFailedEvent.Payload.class);
    TYPE_TO_CLASS = Map.copyOf(map);
  }

  /**
   * Maps Java class → EventType for serialization. Defined explicitly to handle the asymmetry:
   * both {@link Artifact} (DB persistence) and {@link ArtifactDto} (SSE broadcasting) must
   * serialize to a valid event type string, but deserialization always targets {@link Artifact}.
   */
  private static final Map<Class<?>, EventType> CLASS_TO_TYPE;

  static {
    Map<Class<?>, EventType> map = new HashMap<>();
    map.put(Artifact.class, EventType.ARTIFACT_ADDED);
    map.put(ArtifactDto.class, EventType.ARTIFACT_UPDATED);
    map.put(ArtifactRemovedEvent.Payload.class, EventType.ARTIFACT_REMOVED);
    map.put(TaskExecutionStartedEvent.Payload.class, EventType.TASKEXECUTION_STARTED);
    map.put(TaskExecutionCompletedEvent.Payload.class, EventType.TASKEXECUTION_COMPLETED);
    map.put(TaskExecutionFailedEvent.Payload.class, EventType.TASKEXECUTION_FAILED);
    map.put(TodoListUpdatedEvent.Payload.class, EventType.TODO_LIST_UPDATED);
    map.put(ToolCallingEventPayload.class, EventType.TOOL_CALLING);
    map.put(ErrorEventPayload.class, EventType.ERROR);
    map.put(DecomposerStartedEvent.Payload.class, EventType.DECOMPOSER_STARTED);
    map.put(DecomposerFinishedEvent.Payload.class, EventType.DECOMPOSER_FINISHED);
    map.put(DecomposerFailedEvent.Payload.class, EventType.DECOMPOSER_FAILED);
    CLASS_TO_TYPE = Map.copyOf(map);
  }

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
