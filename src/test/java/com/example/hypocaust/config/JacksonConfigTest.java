package com.example.hypocaust.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JacksonConfigTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void testLenientParsing_UnescapedNewlines() throws Exception {
    // This JSON contains a raw newline character (code 10) inside the string value.
    // Standard JSON requires this to be escaped as \n.
    String jsonWithRawNewline = "{\"message\": \"Line 1\nLine 2\"}";

    JsonNode node = objectMapper.readTree(jsonWithRawNewline);
    assertNotNull(node);
    assertEquals("Line 1\nLine 2", node.get("message").asText());
  }
}
