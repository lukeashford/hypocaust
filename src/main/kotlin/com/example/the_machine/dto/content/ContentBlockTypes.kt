package com.example.the_machine.dto.content

/**
 * Constants for ContentBlock type identifiers. These constants ensure consistency between the
 * type() method return values and the @JsonSubTypes.Type name attributes.
 */
object ContentBlockTypes {

  const val TEXT = "text"
  const val MARKDOWN = "markdown"
  const val TOOL_CALL = "tool_call"
  const val TOOL_RESULT = "tool_result"
  const val IMAGE_REF = "image_ref"
  const val FILE_REF = "file_ref"
}