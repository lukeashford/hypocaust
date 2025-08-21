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

class ContentBlockTest {

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

    List<ContentBlock> originalBlocks = List.of(
        new TextContent("Hello, world!"),
        new MarkdownContent("## Header\n\nSome **bold** text"),
        new ToolCallContent("search", toolArgs),
        new ToolResultContent("search", toolResult, "call-123"),
        new ImageRef(imageAssetId),
        new FileRef(fileAssetId, "document.pdf", "application/pdf", 1024000L)
    );

    // Serialize to JSON
    val json = objectMapper.writeValueAsString(originalBlocks);
    System.out.println("[DEBUG_LOG] Serialized JSON: " + json);

    // Deserialize back to objects
    List<ContentBlock> deserializedBlocks = objectMapper.readValue(
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
    assertInstanceOf(TextContent.class, deserializedBlocks.get(0));
    assertInstanceOf(MarkdownContent.class, deserializedBlocks.get(1));
    assertInstanceOf(ToolCallContent.class, deserializedBlocks.get(2));
    assertInstanceOf(ToolResultContent.class, deserializedBlocks.get(3));
    assertInstanceOf(ImageRef.class, deserializedBlocks.get(4));
    assertInstanceOf(FileRef.class, deserializedBlocks.get(5));

    // Verify content - using instanceof checks, no need to test type() method
    // since if instanceof works, the type() method will return the correct constant
    val textContent = (TextContent) deserializedBlocks.get(0);
    assertEquals("Hello, world!", textContent.text());

    val markdownContent = (MarkdownContent) deserializedBlocks.get(1);
    assertEquals("## Header\n\nSome **bold** text", markdownContent.markdown());

    val toolCallContent = (ToolCallContent) deserializedBlocks.get(2);
    assertEquals("search", toolCallContent.name());
    assertEquals("test query", toolCallContent.arguments().get("query").asText());

    val toolResultContent = (ToolResultContent) deserializedBlocks.get(3);
    assertEquals("search", toolResultContent.name());
    assertEquals("call-123", toolResultContent.callId());
    assertEquals("success", toolResultContent.result().get("status").asText());

    val imageRef = (ImageRef) deserializedBlocks.get(4);
    assertEquals(imageAssetId, imageRef.assetId());

    val fileRef = (FileRef) deserializedBlocks.get(5);
    assertEquals(fileAssetId, fileRef.assetId());
    assertEquals("document.pdf", fileRef.filename());
    assertEquals("application/pdf", fileRef.mime());
    assertEquals(1024000L, fileRef.size());

    System.out.println("[DEBUG_LOG] Round-trip test completed successfully");
  }
}