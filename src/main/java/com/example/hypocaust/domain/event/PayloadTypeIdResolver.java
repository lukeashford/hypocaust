package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.ArtifactCancelledEvent.ArtifactCancelledEventPayload;
import com.example.hypocaust.domain.event.ArtifactCreatedEvent.ArtifactCreatedEventPayload;
import com.example.hypocaust.domain.event.ArtifactScheduledEvent.ArtifactScheduledEventPayload;
import com.example.hypocaust.domain.event.ErrorEvent.ErrorEventPayload;
import com.example.hypocaust.domain.event.Event.EventPayload;
import com.example.hypocaust.domain.event.RunCompletedEvent.RunCompletedEventPayload;
import com.example.hypocaust.domain.event.RunScheduledEvent.RunScheduledEventPayload;
import com.example.hypocaust.domain.event.RunStartedEvent.RunStartedEventPayload;
import com.example.hypocaust.domain.event.ToolCallingEvent.ToolCallingEventPayload;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import java.util.Map;

class PayloadTypeIdResolver extends TypeIdResolverBase {

  private static final Map<EventType, Class<? extends EventPayload>> TYPE_TO_CLASS = Map.of(
      EventType.RUN_SCHEDULED, RunScheduledEventPayload.class,
      EventType.RUN_STARTED, RunStartedEventPayload.class,
      EventType.RUN_COMPLETED, RunCompletedEventPayload.class,
      EventType.ARTIFACT_SCHEDULED, ArtifactScheduledEventPayload.class,
      EventType.ARTIFACT_CREATED, ArtifactCreatedEventPayload.class,
      EventType.ARTIFACT_CANCELLED, ArtifactCancelledEventPayload.class,
      EventType.TOOL_CALLING, ToolCallingEventPayload.class,
      EventType.ERROR, ErrorEventPayload.class
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
    Class<? extends EventPayload> payloadClass = TYPE_TO_CLASS.get(eventType);
    return context.constructType(payloadClass);
  }

  @Override
  public JsonTypeInfo.Id getMechanism() {
    return JsonTypeInfo.Id.CUSTOM;
  }
}
