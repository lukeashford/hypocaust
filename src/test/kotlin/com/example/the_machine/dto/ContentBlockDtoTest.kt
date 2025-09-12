package com.example.the_machine.dto

import com.example.the_machine.common.KotlinSerializationConfig
import com.example.the_machine.config.TestDataConfiguration
import com.example.the_machine.dto.content.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class ContentBlockDtoTest {

  private val kotlinxJson = KotlinSerializationConfig.staticJson

  @Test
  fun testRoundTripJsonSerialization() {
    // Create test data with mixed content blocks
    val toolArgs = buildJsonObject {
      put("query", JsonPrimitive("test query"))
      put("limit", JsonPrimitive(10))
    }

    val toolResult = buildJsonObject {
      put("status", JsonPrimitive("success"))
      put("count", JsonPrimitive(5))
    }

    val imageAssetId = TestDataConfiguration.TEST_ARTIFACT_ID
    val fileAssetId = TestDataConfiguration.TEST_INVALID_ID

    val originalBlocks = listOf(
      TextContentDto("Hello, world!"),
      MarkdownContentDto("## Header\n\nSome **bold** text"),
      ToolCallContentDto("search", toolArgs),
      ToolResultContentDto("search", toolResult, "call-123"),
      ImageRefDto(imageAssetId),
      FileRefDto(fileAssetId, "document.pdf", "application/pdf", 1024000L)
    )

    // Serialize to JSON using Kotlinx Serialization
    val json =
      kotlinxJson.encodeToString(ListSerializer(ContentBlockDto.serializer()), originalBlocks)
    println("[DEBUG_LOG] Serialized JSON: $json")

    // Deserialize back to objects using Kotlinx Serialization
    val deserializedBlocks: List<ContentBlockDto> = kotlinxJson.decodeFromString(
      ListSerializer(ContentBlockDto.serializer()),
      json
    )

    // Verify the round-trip worked correctly
    assertEquals(originalBlocks.size, deserializedBlocks.size)

    // Debug: Print actual types and values
    for (i in deserializedBlocks.indices) {
      val block = deserializedBlocks[i]
      println("[DEBUG_LOG] Block $i: ${block.javaClass.simpleName} = $block")
    }

    // Check each block type
    assertInstanceOf(TextContentDto::class.java, deserializedBlocks[0])
    assertInstanceOf(MarkdownContentDto::class.java, deserializedBlocks[1])
    assertInstanceOf(ToolCallContentDto::class.java, deserializedBlocks[2])
    assertInstanceOf(ToolResultContentDto::class.java, deserializedBlocks[3])
    assertInstanceOf(ImageRefDto::class.java, deserializedBlocks[4])
    assertInstanceOf(FileRefDto::class.java, deserializedBlocks[5])

    // Verify content - using instanceof checks, no need to test type() method
    // since if instanceof works, the type() method will return the correct constant
    val textContent = deserializedBlocks[0] as TextContentDto
    assertEquals("Hello, world!", textContent.text)

    val markdownContent = deserializedBlocks[1] as MarkdownContentDto
    assertEquals("## Header\n\nSome **bold** text", markdownContent.markdown)

    val toolCallContent = deserializedBlocks[2] as ToolCallContentDto
    assertEquals("search", toolCallContent.name)
    assertEquals(
      "test query",
      toolCallContent.arguments.jsonObject["query"]?.jsonPrimitive?.content
    )

    val toolResultContent = deserializedBlocks[3] as ToolResultContentDto
    assertEquals("search", toolResultContent.name)
    assertEquals("call-123", toolResultContent.callId)
    assertEquals("success", toolResultContent.result.jsonObject["status"]?.jsonPrimitive?.content)

    val imageRef = deserializedBlocks[4] as ImageRefDto
    assertEquals(imageAssetId, imageRef.assetId)

    val fileRef = deserializedBlocks[5] as FileRefDto
    assertEquals(fileAssetId, fileRef.assetId)
    assertEquals("document.pdf", fileRef.filename)
    assertEquals("application/pdf", fileRef.mime)
    assertEquals(1024000L, fileRef.size)

    println("[DEBUG_LOG] Round-trip test completed successfully")
  }
}