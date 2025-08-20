package com.example.the_machine.dto;

/**
 * Constants for ContentBlock type identifiers. These constants ensure consistency between the
 * type() method return values and the @JsonSubTypes.Type name attributes.
 */
public final class ContentBlockTypes {

  public static final String TEXT = "text";
  public static final String MARKDOWN = "markdown";
  public static final String TOOL_CALL = "tool_call";
  public static final String TOOL_RESULT = "tool_result";
  public static final String IMAGE_REF = "image_ref";
  public static final String FILE_REF = "file_ref";

  private ContentBlockTypes() {
    // Prevent instantiation
  }
}