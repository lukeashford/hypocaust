package com.example.the_machine.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.example.the_machine.common.Json;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.Test;

class ContentBlockDtoTest {

  private final ObjectMapper objectMapper = Json.getObjectMapper();

  @Test
  void testRoundTripJsonSerialization() throws Exception {
    // Create test data with mixed content blocks
    val toolArgs = objectMapper.createObjectNode();
    toolArgs.put("query", "test query");
    toolArgs.put("limit", 10);

    val toolResult = objectMapper.createObjectNode();
    toolResult.put("status", "success");
    toolResult.put("count", 5);

    val imageAssetId = UUID.randomUUID();
    val fileAssetId = UUID.randomUUID();

    List<ContentBlockDto> originalBlocks = List.of(
        new TextContentDto("Hello, world!"),
        new MarkdownContentDto("## Header\n\nSome **bold** text"),
        new ToolCallContentDto("search", toolArgs),
        new ToolResultContentDto("search", toolResult, "call-123"),
        new ImageRefDto(imageAssetId),
        new FileRefDto(fileAssetId, "document.pdf", "application/pdf", 1024000L)
    );

    // Serialize to JSON
    val json = objectMapper.writeValueAsString(originalBlocks);
    System.out.println("[DEBUG_LOG] Serialized JSON: " + json);

    // Deserialize back to objects
    List<ContentBlockDto> deserializedBlocks = objectMapper.readValue(
        json,
        new TypeReference<>() {
        }
    );

    // Verify the round-trip worked correctly
    assertEquals(originalBlocks.size(), deserializedBlocks.size());

    // Debug: Print actual types and values
    for (int i = 0; i < deserializedBlocks.size(); i++) {
      val block = deserializedBlocks.get(i);
      System.out.println(
          "[DEBUG_LOG] Block " + i + ": " + block.getClass().getSimpleName() + " = " + block);
    }

    // Check each block type
    assertInstanceOf(TextContentDto.class, deserializedBlocks.get(0));
    assertInstanceOf(MarkdownContentDto.class, deserializedBlocks.get(1));
    assertInstanceOf(ToolCallContentDto.class, deserializedBlocks.get(2));
    assertInstanceOf(ToolResultContentDto.class, deserializedBlocks.get(3));
    assertInstanceOf(ImageRefDto.class, deserializedBlocks.get(4));
    assertInstanceOf(FileRefDto.class, deserializedBlocks.get(5));

    // Verify content - using instanceof checks, no need to test type() method
    // since if instanceof works, the type() method will return the correct constant
    val textContent = (TextContentDto) deserializedBlocks.get(0);
    assertEquals("Hello, world!", textContent.text());

    val markdownContent = (MarkdownContentDto) deserializedBlocks.get(1);
    assertEquals("## Header\n\nSome **bold** text", markdownContent.markdown());

    val toolCallContent = (ToolCallContentDto) deserializedBlocks.get(2);
    assertEquals("search", toolCallContent.name());
    assertEquals("test query", toolCallContent.arguments().get("query").asText());

    val toolResultContent = (ToolResultContentDto) deserializedBlocks.get(3);
    assertEquals("search", toolResultContent.name());
    assertEquals("call-123", toolResultContent.callId());
    assertEquals("success", toolResultContent.result().get("status").asText());

    val imageRef = (ImageRefDto) deserializedBlocks.get(4);
    assertEquals(imageAssetId, imageRef.assetId());

    val fileRef = (FileRefDto) deserializedBlocks.get(5);
    assertEquals(fileAssetId, fileRef.assetId());
    assertEquals("document.pdf", fileRef.filename());
    assertEquals("application/pdf", fileRef.mime());
    assertEquals(1024000L, fileRef.size());

    System.out.println("[DEBUG_LOG] Round-trip test completed successfully");
  }
}