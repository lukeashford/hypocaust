package com.example.the_machine.config

import com.example.the_machine.domain.EventType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class EventTypeConverter : AttributeConverter<EventType, String> {

  override fun convertToDatabaseColumn(eventType: EventType?): String? {
    return eventType?.serialName
  }

  override fun convertToEntityAttribute(value: String?): EventType? {
    return EventType.entries.find { it.serialName == value }
  }
}