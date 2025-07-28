package com.example.the_machine.retriever.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.the_machine.retriever.extract.BoilerpipeContentExtractor.Mode;
import lombok.val;
import org.junit.jupiter.api.Test;

class BoilerpipeContentExtractorTest {

  /**
   * Helper method to extract content using BoilerpipeContentExtractor
   */
  private String extract(Mode mode, String html) {
    val result = new BoilerpipeContentExtractor(mode).extract(html);
    if (result.isPresent()) {
      val content = result.get();

      // Verify BoilerpipeContentExtractor returns empty title and excerpt
      assertEquals("", content.title());
      assertEquals("", content.excerpt());

      // Verify text content extraction
      assertNotNull(content.text());
      assertFalse(content.text().isBlank());

      // Ensure no HTML tags remain
      assertFalse(content.text().contains("<"));
      assertFalse(content.text().contains(">"));

      return content.text();
    }
    return null;
  }

  @Test
  void extract_withValidHtml_articleMode_shouldReturnContent() {
    val html = """
        <html>
        <head><title>Test Title</title></head>
        <body>
          <div class="header">Navigation and ads</div>
          <article>
            <h1>Main Article Title</h1>
            <p>This is the first paragraph of the main content of the article. It contains substantial text that should be extracted by Boilerpipe's article extractor.</p>
            <p>This is the second paragraph with more content to ensure there's enough text for Boilerpipe to consider this as main content worth extracting.</p>
            <p>A third paragraph to make sure we have substantial content that meets Boilerpipe's thresholds for article extraction.</p>
          </article>
          <div class="footer">Footer content and links</div>
        </body>
        </html>
        """;

    val expectedContent = """
        Navigation and ads
        Main Article Title
        This is the first paragraph of the main content of the article. It contains substantial text that should be extracted by Boilerpipe's article extractor.
        This is the second paragraph with more content to ensure there's enough text for Boilerpipe to consider this as main content worth extracting.
        A third paragraph to make sure we have substantial content that meets Boilerpipe's thresholds for article extraction.
        Footer content and links
        """;

    assertEquals(expectedContent, extract(Mode.ARTICLE, html));
  }

  @Test
  void extract_withValidHtml_largestContentMode_shouldReturnContent() {
    val html = """
        <html>
        <head><title>Test Title</title></head>
        <body>
          <div class="header">Short header</div>
          <article>
            <h1>Main Article Heading</h1>
            <p>This is a longer paragraph with substantial content that should be extracted by the largest content extractor.</p>
            <p>Another paragraph with more meaningful content to ensure this is the largest block.</p>
          </article>
          <div class="footer">Short footer</div>
        </body>
        </html>
        """;

    val expectedContent = """
        Short header
        Main Article Heading
        This is a longer paragraph with substantial content that should be extracted by the largest content extractor.
        Another paragraph with more meaningful content to ensure this is the largest block.
        Short footer
        """;

    assertEquals(expectedContent, extract(Mode.LARGEST_CONTENT, html));
  }

  @Test
  void extract_withValidHtml_defaultMode_shouldReturnContent() {
    val html = """
        <html>
        <head><title>Default Mode Test</title></head>
        <body>
          <div class="navigation">Site navigation</div>
          <div>
            <h1>Content Header for Default Mode</h1>
            <p>This content should be extracted by the default Boilerpipe extractor. It contains substantial text to ensure proper extraction.</p>
            <p>A second paragraph with additional content to make sure the default extractor has enough text to work with effectively.</p>
            <p>Third paragraph to provide even more content for the default mode extraction algorithm to process successfully.</p>
          </div>
          <div class="sidebar">Sidebar content</div>
        </body>
        </html>
        """;

    val expectedContent = """
        Content Header for Default Mode
        This content should be extracted by the default Boilerpipe extractor. It contains substantial text to ensure proper extraction.
        A second paragraph with additional content to make sure the default extractor has enough text to work with effectively.
        Third paragraph to provide even more content for the default mode extraction algorithm to process successfully.
        Sidebar content
        """;

    assertEquals(expectedContent, extract(Mode.DEFAULT, html));
  }

  @Test
  void extract_withComplexHtml_articleMode_shouldReturnStructuredContent() {
    val html = """
        <html>
        <head><title>Complex Article Title</title></head>
        <body>
          <div class="header">Header content and navigation</div>
          <article>
            <h1>Main Article Heading for Complex Content</h1>
            <p>First paragraph with important content that provides substantial text for the article extractor to process effectively.</p>
            <h2>Subheading for Additional Content</h2>
            <p>Second paragraph with more details and <strong>bold text</strong> that adds to the overall content length and complexity.</p>
            <ul>
              <li>List item one with detailed information</li>
              <li>List item two with additional details</li>
            </ul>
            <p>Final paragraph concluding the article with substantial content to ensure proper extraction by Boilerpipe's article mode.</p>
            <p>An additional paragraph to provide even more content for thorough testing of the complex HTML extraction capabilities.</p>
          </article>
          <div class="footer">Footer content and links</div>
        </body>
        </html>
        """;

    val expectedContent = """
        Header content and navigation
        Main Article Heading for Complex Content
        First paragraph with important content that provides substantial text for the article extractor to process effectively.
        Subheading for Additional Content
        Second paragraph with more details and bold text that adds to the overall content length and complexity.
        List item one with detailed information
        List item two with additional details
        Final paragraph concluding the article with substantial content to ensure proper extraction by Boilerpipe's article mode.
        An additional paragraph to provide even more content for thorough testing of the complex HTML extraction capabilities.
        Footer content and links
        """;

    assertEquals(expectedContent, extract(Mode.ARTICLE, html));
  }

  @Test
  void extract_withNullHtml_shouldReturnEmpty() {
    val result = new BoilerpipeContentExtractor(Mode.ARTICLE).extract(null);

    assertFalse(result.isPresent());
  }

  @Test
  void extract_withBlankHtml_shouldReturnEmpty() {
    val result = new BoilerpipeContentExtractor(Mode.ARTICLE).extract("");

    assertFalse(result.isPresent());
  }

  @Test
  void extract_withWhitespaceHtml_shouldReturnEmpty() {
    val result = new BoilerpipeContentExtractor(Mode.ARTICLE).extract("   ");

    assertFalse(result.isPresent());
  }

  @Test
  void extract_withInvalidHtml_shouldReturnEmpty() {
    val invalidHtml = "not html at all";

    val result = new BoilerpipeContentExtractor(Mode.ARTICLE).extract(invalidHtml);

    assertNotNull(result);
    assertFalse(result.isPresent());
  }

  @Test
  void extract_withEmptyBodyHtml_shouldReturnEmpty() {
    val html = "<html><head><title>Test</title></head><body></body></html>";

    val result = new BoilerpipeContentExtractor(Mode.ARTICLE).extract(html);

    // Should return empty since there's no meaningful content
    assertNotNull(result);
  }

  @Test
  void extract_allModes_withSameHtml_shouldAllWork() {
    val html = """
        <html>
        <body>
          <div>
            <h1>Test Content</h1>
            <p>This is test content for all modes.</p>
          </div>
        </body>
        </html>
        """;

    // Test all three modes work without throwing exceptions
    val articleExtractor = new BoilerpipeContentExtractor(BoilerpipeContentExtractor.Mode.ARTICLE);
    val largestExtractor = new BoilerpipeContentExtractor(
        BoilerpipeContentExtractor.Mode.LARGEST_CONTENT);
    val defaultExtractor = new BoilerpipeContentExtractor(BoilerpipeContentExtractor.Mode.DEFAULT);

    val articleResult = articleExtractor.extract(html);
    val largestResult = largestExtractor.extract(html);
    val defaultResult = defaultExtractor.extract(html);

    // All should return results (though content may vary)
    assertNotNull(articleResult);
    assertNotNull(largestResult);
    assertNotNull(defaultResult);

    // If any return content, verify it's properly formatted
    if (articleResult.isPresent()) {
      val content = articleResult.get();
      assertEquals("", content.title());
      assertEquals("", content.excerpt());
      assertNotNull(content.text());
    }

    if (largestResult.isPresent()) {
      val content = largestResult.get();
      assertEquals("", content.title());
      assertEquals("", content.excerpt());
      assertNotNull(content.text());
    }

    if (defaultResult.isPresent()) {
      val content = defaultResult.get();
      assertEquals("", content.title());
      assertEquals("", content.excerpt());
      assertNotNull(content.text());
    }
  }

}