package com.example.the_machine.config;

import com.example.the_machine.domain.event.EventType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EventTypeConverter implements AttributeConverter<EventType, String> {

  @Override
  public String convertToDatabaseColumn(EventType eventType) {
    return eventType != null ? eventType.getValue() : null;
  }

  @Override
  public EventType convertToEntityAttribute(String value) {
    return value != null ? EventType.fromValue(value) : null;
  }
}