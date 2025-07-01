package com.example.shared.contract

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Represents the type of source to scrape.
 */
enum class SourceType(
  @JsonValue
  val stringValue: String
) {

  Web("web"),
  YouTube("youtube"),
  Twitter("twitter");

  override fun toString(): String = stringValue

  companion object {

    @JsonCreator
    @JvmStatic
    fun fromString(value: String): SourceType {
      return SourceType.entries.find { it.stringValue == value }
        ?: throw IllegalArgumentException("Unknown source type: $value")
    }
  }
}
