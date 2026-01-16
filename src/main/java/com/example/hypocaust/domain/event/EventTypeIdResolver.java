package com.example.hypocaust.domain.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import java.util.Map;

class EventTypeIdResolver extends TypeIdResolverBase {

  private static final Map<EventType, Class<? extends Event<?>>> TYPE_TO_CLASS = Map.of(
      EventType.RUN_SCHEDULED, RunScheduledEvent.class,
      EventType.RUN_STARTED, RunStartedEvent.class,
      EventType.RUN_COMPLETED, RunCompletedEvent.class,
      EventType.ARTIFACT_SCHEDULED, ArtifactScheduledEvent.class,
      EventType.ARTIFACT_CREATED, ArtifactCreatedEvent.class,
      EventType.ARTIFACT_CANCELLED, ArtifactCancelledEvent.class,
      EventType.TOOL_CALLING, ToolCallingEvent.class,
      EventType.ERROR, ErrorEvent.class
  );

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