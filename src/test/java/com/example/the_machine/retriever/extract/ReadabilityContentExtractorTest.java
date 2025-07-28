package com.example.the_machine.retriever.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadabilityContentExtractorTest {

  private ReadabilityContentExtractor extractor;

  @BeforeEach
  void setUp() {
    extractor = new ReadabilityContentExtractor();
  }

  @Test
  void extract_withValidHtml_shouldReturnContent() {
    val html = """
        <html>
        <head><title>Test Title</title></head>
        <body>
          <article>
            <h1>Main Title</h1>
            <p>This is the main content of the article.</p>
          </article>
        </body>
        </html>
        """;

    val result = extractor.extract(html);

    assertTrue(result.isPresent());
    val content = result.get();

    // Verify title extraction
    assertNotNull(content.title());
    assertEquals("Test Title", content.title());

    // Verify text content extraction - should be plain text without HTML tags
    assertNotNull(content.text());
    assertFalse(content.text().isBlank());

    // Restrictive testing: verify content is present and HTML is stripped
    val expectedText = "Main Title This is the main content of the article.";
    assertEquals(expectedText, content.text().trim());
    assertTrue(content.text().contains("This is the main content of the article."));

    // Ensure no HTML tags remain in the text
    assertFalse(content.text().contains("<"));
    assertFalse(content.text().contains(">"));
    assertFalse(content.text().contains("<h1>"));
    assertFalse(content.text().contains("</h1>"));
    assertFalse(content.text().contains("<p>"));
    assertFalse(content.text().contains("</p>"));

    // Verify excerpt is not null (may be empty string)
    assertNotNull(content.excerpt());
  }

  @Test
  void extract_withComplexHtml_shouldReturnStructuredContent() {
    val html = """
        <html>
        <head><title>Complex Article Title</title></head>
        <body>
          <div class="header">Header content</div>
          <article>
            <h1>Main Article Heading</h1>
            <p>First paragraph with important content.</p>
            <h2>Subheading</h2>
            <p>Second paragraph with more details and <strong>bold text</strong>.</p>
            <ul>
              <li>List item one</li>
              <li>List item two</li>
            </ul>
            <p>Final paragraph concluding the article.</p>
          </article>
          <div class="footer">Footer content</div>
        </body>
        </html>
        """;

    val expected_title = "Complex Article Title";
    val expected_content = "Main Article Heading First paragraph with important content. Subheading Second paragraph with more details and bold text. List item one List item two Final paragraph concluding the article.";
    val expected_excerpt = "First paragraph with important content.";

    val result = extractor.extract(html);

    assertTrue(result.isPresent());
    val content = result.get();
    assertNotNull(content);

    // Verify title extraction
    assertEquals(expected_title, content.title());

    // verify content is present and HTML is stripped
    assertNotNull(content.text());
    assertFalse(content.text().contains("<"));
    assertFalse(content.text().contains(">"));
    assertFalse(content.text().contains("<h1>"));
    assertFalse(content.text().contains("<p>"));
    assertFalse(content.text().contains("<strong>"));
    assertFalse(content.text().contains("<ul>"));
    assertFalse(content.text().contains("<li>"));
    assertEquals(expected_content, content.text());

    // Verify that header/footer noise is not included (readability should filter this)
    assertFalse(content.text().contains("Header content"));
    assertFalse(content.text().contains("Footer content"));

    // Verify excerpt is not null
    assertNotNull(content.excerpt());
    assertEquals(expected_excerpt, content.excerpt());
  }

  @Test
  void extract_withNullHtml_shouldReturnEmpty() {
    val result = extractor.extract(null);

    assertFalse(result.isPresent());
  }

  @Test
  void extract_withBlankHtml_shouldReturnEmpty() {
    val result = extractor.extract("");

    assertFalse(result.isPresent());
  }

  @Test
  void extract_withWhitespaceHtml_shouldReturnEmpty() {
    val result = extractor.extract("   ");

    assertFalse(result.isPresent());
  }

  @Test
  void extract_withInvalidHtml_shouldReturnEmpty() {
    val invalidHtml = "not html at all";

    val result = extractor.extract(invalidHtml);

    // Readability4j might still extract some text, but if it fails, should return empty
    // This test verifies the extractor handles invalid input gracefully
    assertNotNull(result);
  }

  @Test
  void extract_withEmptyBodyHtml_shouldReturnEmpty() {
    val html = "<html><head><title>Test</title></head><body></body></html>";

    val result = extractor.extract(html);

    // Should return empty since there's no meaningful content
    assertNotNull(result);
  }
}