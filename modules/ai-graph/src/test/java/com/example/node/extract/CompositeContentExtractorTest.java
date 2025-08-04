package com.example.node.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.tool.extract.CompositeContentExtractor;
import com.example.tool.extract.ContentExtractor;
import com.example.tool.extract.PageContent;
import com.example.tool.extract.ReadabilityContentExtractor;
import java.util.List;
import java.util.Optional;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompositeContentExtractorTest {

  @Mock
  private ContentExtractor firstExtractor;

  @Mock
  private ContentExtractor secondExtractor;

  @Mock
  private ContentExtractor thirdExtractor;

  private CompositeContentExtractor compositeExtractor;

  @BeforeEach
  void setUp() {
    compositeExtractor = new CompositeContentExtractor(
        List.of(firstExtractor, secondExtractor, thirdExtractor));
  }

  @Test
  void extract_firstExtractorSucceeds_shouldReturnFirstResult() {
    val html = "<html><body>Test content</body></html>";
    val expectedContent = new PageContent("Title", "Content", "Excerpt");

    when(firstExtractor.extract(html)).thenReturn(Optional.of(expectedContent));

    val result = compositeExtractor.extract(html);

    assertTrue(result.isPresent());
    assertEquals(expectedContent, result.get());

    // Verify only first extractor was called
    verify(firstExtractor).extract(html);
    verify(secondExtractor, never()).extract(html);
    verify(thirdExtractor, never()).extract(html);
  }

  @Test
  void extract_firstFailsSecondSucceeds_shouldReturnSecondResult() {
    val html = "<html><body>Test content</body></html>";
    val expectedContent = new PageContent("Title2", "Content2", "Excerpt2");

    when(firstExtractor.extract(html)).thenReturn(Optional.empty());
    when(secondExtractor.extract(html)).thenReturn(Optional.of(expectedContent));

    val result = compositeExtractor.extract(html);

    assertTrue(result.isPresent());
    assertEquals(expectedContent, result.get());

    // Verify first two extractors were called, but not third
    verify(firstExtractor).extract(html);
    verify(secondExtractor).extract(html);
    verify(thirdExtractor, never()).extract(html);
  }

  @Test
  void extract_firstTwoFailThirdSucceeds_shouldReturnThirdResult() {
    val html = "<html><body>Test content</body></html>";
    val expectedContent = new PageContent("Title3", "Content3", "Excerpt3");

    when(firstExtractor.extract(html)).thenReturn(Optional.empty());
    when(secondExtractor.extract(html)).thenReturn(Optional.empty());
    when(thirdExtractor.extract(html)).thenReturn(Optional.of(expectedContent));

    val result = compositeExtractor.extract(html);

    assertTrue(result.isPresent());
    assertEquals(expectedContent, result.get());

    // Verify all extractors were called
    verify(firstExtractor).extract(html);
    verify(secondExtractor).extract(html);
    verify(thirdExtractor).extract(html);
  }

  @Test
  void extract_allExtractorsFail_shouldReturnEmpty() {
    val html = "<html><body>Test content</body></html>";

    when(firstExtractor.extract(html)).thenReturn(Optional.empty());
    when(secondExtractor.extract(html)).thenReturn(Optional.empty());
    when(thirdExtractor.extract(html)).thenReturn(Optional.empty());

    val result = compositeExtractor.extract(html);

    assertFalse(result.isPresent());

    // Verify all extractors were called
    verify(firstExtractor).extract(html);
    verify(secondExtractor).extract(html);
    verify(thirdExtractor).extract(html);
  }

  @Test
  void extract_withEmptyChain_shouldReturnEmpty() {
    val emptyComposite = new CompositeContentExtractor(List.of());
    val html = "<html><body>Test content</body></html>";

    val result = emptyComposite.extract(html);

    assertFalse(result.isPresent());
  }

  @Test
  void extract_withRealExtractors_shouldTestFallbackOrder() {
    // Test with real extractors to verify integration
    val readability = new ReadabilityContentExtractor();

    val realComposite = new CompositeContentExtractor(List.of(readability));

    val html = "<html><head><title>Test Title</title></head><body><article><h1>Main Title</h1><p>This is substantial content that should be extracted by at least one of the extractors.</p></article></body></html>";

    val result = realComposite.extract(html);

    // The readability extractor should succeed with this HTML
    assertTrue(result.isPresent());
    val content = result.get();
    assertNotNull(content.text());
    assertFalse(content.text().isBlank());
  }
}