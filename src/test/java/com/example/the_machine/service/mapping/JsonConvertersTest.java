package com.example.the_machine.service.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.the_machine.dto.ContentBlockDto;
import com.example.the_machine.dto.MarkdownContentDto;
import com.example.the_machine.dto.TextContentDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JsonConvertersTest {

  @Autowired
  private JsonConverters jsonConverters;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void testContentBlockRoundTrip() throws Exception {
    // Given - list of different content block types
    List<ContentBlockDto> original = List.of(
        new TextContentDto("Hello World"),
        new MarkdownContentDto("# Title\n\nSome content")
    );

    // When - convert to JSON and back
    val json = jsonConverters.blocksToJson(original);
    val roundTrip = jsonConverters.blocksFromJson(json);

    // Then - should be identical
    assertNotNull(roundTrip);
    assertEquals(original.size(), roundTrip.size());
    assertEquals(objectMapper.writeValueAsString(original),
        objectMapper.writeValueAsString(roundTrip));
  }

  @Test
  void testContentBlockNullHandling() {
    // Test null list -> null JSON
    assertNull(jsonConverters.blocksToJson(null));

    // Test null JSON -> empty list
    assertEquals(List.of(), jsonConverters.blocksFromJson(null));

    // Test JSON null node -> empty list
    assertEquals(List.of(), jsonConverters.blocksFromJson(objectMapper.nullNode()));
  }

  @Test
  void testUUIDListRoundTrip() throws Exception {
    // Given - list of UUIDs
    List<UUID> original = List.of(
        UUID.randomUUID(),
        UUID.randomUUID()
    );

    // When - convert to JSON and back
    val json = jsonConverters.uuidsToJson(original);
    val roundTrip = jsonConverters.uuidsFromJson(json);

    // Then - should be identical
    assertNotNull(roundTrip);
    assertEquals(original.size(), roundTrip.size());
    assertEquals(objectMapper.writeValueAsString(original),
        objectMapper.writeValueAsString(roundTrip));
  }

  @Test
  void testUUIDListNullHandling() {
    // Test null list -> null JSON
    assertNull(jsonConverters.uuidsToJson(null));

    // Test null JSON -> empty list
    assertEquals(List.of(), jsonConverters.uuidsFromJson(null));

    // Test JSON null node -> empty list
    assertEquals(List.of(), jsonConverters.uuidsFromJson(objectMapper.nullNode()));
  }

  @Test
  void testEmptyListHandling() {
    // Test empty list -> [] JSON -> empty list
    List<ContentBlockDto> emptyBlocks = List.of();
    val blocksJson = jsonConverters.blocksToJson(emptyBlocks);
    val blocksRoundTrip = jsonConverters.blocksFromJson(blocksJson);

    assertEquals(emptyBlocks, blocksRoundTrip);

    // Test empty UUID list
    List<UUID> emptyUuids = List.of();
    val uuidsJson = jsonConverters.uuidsToJson(emptyUuids);
    val uuidsRoundTrip = jsonConverters.uuidsFromJson(uuidsJson);

    assertEquals(emptyUuids, uuidsRoundTrip);
  }
}