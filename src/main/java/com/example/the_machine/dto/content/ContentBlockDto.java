package com.example.the_machine.dto.content;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextContentDto.class, name = ContentBlockTypes.TEXT),
    @JsonSubTypes.Type(value = MarkdownContentDto.class, name = ContentBlockTypes.MARKDOWN),
    @JsonSubTypes.Type(value = ToolCallContentDto.class, name = ContentBlockTypes.TOOL_CALL),
    @JsonSubTypes.Type(value = ToolResultContentDto.class, name = ContentBlockTypes.TOOL_RESULT),
    @JsonSubTypes.Type(value = ImageRefDto.class, name = ContentBlockTypes.IMAGE_REF),
    @JsonSubTypes.Type(value = FileRefDto.class, name = ContentBlockTypes.FILE_REF)
})
public sealed interface ContentBlockDto permits
    TextContentDto,
    MarkdownContentDto,
    ToolCallContentDto,
    ToolResultContentDto,
    ImageRefDto,
    FileRefDto {

  /**
   * Returns the type identifier for this content block. This should match the constant defined in
   * ContentBlockTypes.
   */
  @JsonGetter("type")
  String type();
}