package com.example.the_machine.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextContent.class, name = ContentBlockTypes.TEXT),
    @JsonSubTypes.Type(value = MarkdownContent.class, name = ContentBlockTypes.MARKDOWN),
    @JsonSubTypes.Type(value = ToolCallContent.class, name = ContentBlockTypes.TOOL_CALL),
    @JsonSubTypes.Type(value = ToolResultContent.class, name = ContentBlockTypes.TOOL_RESULT),
    @JsonSubTypes.Type(value = ImageRef.class, name = ContentBlockTypes.IMAGE_REF),
    @JsonSubTypes.Type(value = FileRef.class, name = ContentBlockTypes.FILE_REF)
})
public sealed interface ContentBlock
    permits TextContent, MarkdownContent, ToolCallContent, ToolResultContent, ImageRef, FileRef {

  /**
   * Returns the type identifier for this content block. This should match the constant defined in
   * ContentBlockTypes.
   */
  @JsonGetter("type")
  String type();
}