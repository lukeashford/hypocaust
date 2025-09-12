package com.example.the_machine.service.mapping

import com.example.the_machine.config.TestDataConfiguration.TEST_RUN_ID
import com.example.the_machine.config.TestDataConfiguration.TEST_THREAD_ID
import com.example.the_machine.dto.content.ContentBlockDto
import com.example.the_machine.dto.content.MarkdownContentDto
import com.example.the_machine.dto.content.TextContentDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest
class JsonConvertersTest {

  @Autowired
  private lateinit var jsonConverters: JsonConverters

  @Test
  fun testContentBlockRoundTrip() {
    // Given - list of different content block types
    val original = listOf(
      TextContentDto("Hello World"),
      MarkdownContentDto("# Title\n\nSome content")
    )

    // When - convert to JSON and back
    val json = jsonConverters.blocksToJson(original)
    val roundTrip = jsonConverters.blocksFromJson(json)

    // Then - should be identical
    assertNotNull(roundTrip)
    assertEquals(original.size, roundTrip.size)
    assertEquals(original, roundTrip)
  }

  @Test
  fun testUUIDListRoundTrip() {
    // Given - list of UUIDs
    val original = listOf(
      TEST_THREAD_ID,
      TEST_RUN_ID
    )

    // When - convert to JSON and back
    val json = jsonConverters.uuidsToJson(original)
    val roundTrip = jsonConverters.uuidsFromJson(json)

    // Then - should be identical
    assertNotNull(roundTrip)
    assertEquals(original.size, roundTrip.size)
    assertEquals(original, roundTrip)
  }

  @Test
  fun testUUIDListNullHandling() {
    // Test null list -> null JSON
    assertNull(jsonConverters.uuidsToJson(null))

    // Test null JSON -> empty list
    assertEquals(emptyList<UUID>(), jsonConverters.uuidsFromJson(null))

    // Test empty JSON -> empty list
    assertEquals(emptyList<UUID>(), jsonConverters.uuidsFromJson(null))
  }

  @Test
  fun testEmptyListHandling() {
    // Test empty list -> [] JSON -> empty list
    val emptyBlocks = emptyList<ContentBlockDto>()
    val blocksJson = jsonConverters.blocksToJson(emptyBlocks)
    val blocksRoundTrip = jsonConverters.blocksFromJson(blocksJson)

    assertEquals(emptyBlocks, blocksRoundTrip)

    // Test empty UUID list
    val emptyUuids = emptyList<UUID>()
    val uuidsJson = jsonConverters.uuidsToJson(emptyUuids)
    val uuidsRoundTrip = jsonConverters.uuidsFromJson(uuidsJson)

    assertEquals(emptyUuids, uuidsRoundTrip)
  }
}