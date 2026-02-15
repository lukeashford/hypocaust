package com.example.hypocaust.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JsonUtilsTest {

  @Test
  void testExtractJson_MarkdownJson() {
    String response = """
        Here is the plan:
        ```json
        {
          "title": "Cool Image",
          "description": "A very cool image"
        }
        ```
        I hope you like it!""";
    String expected = """
        {
          "title": "Cool Image",
          "description": "A very cool image"
        }""";
    assertEquals(expected, JsonUtils.extractJson(response));
  }

  @Test
  void testExtractJson_MarkdownGeneric() {
    String response = """
        Plan:
        ```
        {"key": "value"}
        ```""";
    assertEquals("{\"key\": \"value\"}", JsonUtils.extractJson(response));
  }

  @Test
  void testExtractJson_BareObject() {
    String response = "Sure, here it is: {\"title\": \"test\"} and some more text.";
    assertEquals("{\"title\": \"test\"}", JsonUtils.extractJson(response));
  }

  @Test
  void testExtractJson_NoJson() {
    String response = "Hello World";
    assertEquals("Hello World", JsonUtils.extractJson(response));
  }

  @Test
  void testExtractJson_Empty() {
    assertEquals("{}", JsonUtils.extractJson(""));
    assertEquals("{}", JsonUtils.extractJson(null));
  }
}
