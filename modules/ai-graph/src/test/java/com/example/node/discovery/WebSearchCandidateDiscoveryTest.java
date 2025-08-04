package com.example.node.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.tool.discovery.WebSearchCandidateDiscovery;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for WebSearchCandidateDiscovery. Tests the core functionality, edge cases, and proper
 * handling of dependencies.
 */
@ExtendWith(MockitoExtension.class)
class WebSearchCandidateDiscoveryTest {

  @Mock
  private WebSearchEngine webSearchEngine;

  @Mock
  private WebSearchResults webSearchResults;

  private WebSearchCandidateDiscovery webSearchCandidateDiscovery;

  @BeforeEach
  void setUp() {
    webSearchCandidateDiscovery = new WebSearchCandidateDiscovery(webSearchEngine);
  }

  @Test
  void find_shouldReturnListOfURIs_whenSearchReturnsResults() {
    // Given
    String query = "test query";
    int maxResults = 5;

    List<WebSearchOrganicResult> mockResults = Arrays.asList(
        createMockResult("https://example1.com"),
        createMockResult("https://example2.com"),
        createMockResult("https://example3.com")
    );

    when(webSearchResults.results()).thenReturn(mockResults);
    when(webSearchEngine.search(any(WebSearchRequest.class))).thenReturn(webSearchResults);

    // When
    List<URI> result = webSearchCandidateDiscovery.find(query, maxResults);

    // Then
    assertEquals(3, result.size());
    assertEquals(URI.create("https://example1.com"), result.get(0));
    assertEquals(URI.create("https://example2.com"), result.get(1));
    assertEquals(URI.create("https://example3.com"), result.get(2));
  }

  @Test
  void find_shouldReturnEmptyList_whenSearchReturnsNoResults() {
    // Given
    String query = "test query";
    int maxResults = 5;

    when(webSearchResults.results()).thenReturn(Collections.emptyList());
    when(webSearchEngine.search(any(WebSearchRequest.class))).thenReturn(webSearchResults);

    // When
    List<URI> result = webSearchCandidateDiscovery.find(query, maxResults);

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  void find_shouldUseRequestedMax_whenRequestedMaxIsWithinLimit() {
    // Given
    String query = "test query";
    int maxResults = 10; // Within MAX_SEARCH_RESULTS (30)

    ArgumentCaptor<WebSearchRequest> requestCaptor = ArgumentCaptor.forClass(
        WebSearchRequest.class);
    when(webSearchResults.results()).thenReturn(Collections.emptyList());
    when(webSearchEngine.search(requestCaptor.capture())).thenReturn(webSearchResults);

    // When
    webSearchCandidateDiscovery.find(query, maxResults);

    // Then
    WebSearchRequest capturedRequest = requestCaptor.getValue();
    assertEquals(10, capturedRequest.maxResults());
    assertEquals(query, capturedRequest.searchTerms());
  }

  @Test
  void find_shouldHandleZeroMaxResults() {
    // Given
    String query = "test query";
    int maxResults = 0;

    ArgumentCaptor<WebSearchRequest> requestCaptor = ArgumentCaptor.forClass(
        WebSearchRequest.class);
    when(webSearchResults.results()).thenReturn(Collections.emptyList());
    when(webSearchEngine.search(requestCaptor.capture())).thenReturn(webSearchResults);

    // When
    List<URI> result = webSearchCandidateDiscovery.find(query, maxResults);

    // Then
    WebSearchRequest capturedRequest = requestCaptor.getValue();
    assertEquals(0, capturedRequest.maxResults());
    assertTrue(result.isEmpty());
  }

  @Test
  void find_shouldHandleNegativeMaxResults() {
    // Given
    String query = "test query";
    int maxResults = -5;

    ArgumentCaptor<WebSearchRequest> requestCaptor = ArgumentCaptor.forClass(
        WebSearchRequest.class);
    when(webSearchResults.results()).thenReturn(Collections.emptyList());
    when(webSearchEngine.search(requestCaptor.capture())).thenReturn(webSearchResults);

    // When
    List<URI> result = webSearchCandidateDiscovery.find(query, maxResults);

    // Then
    WebSearchRequest capturedRequest = requestCaptor.getValue();
    assertEquals(-5, capturedRequest.maxResults()); // Math.min preserves negative value
    assertTrue(result.isEmpty());
  }

  @Test
  void find_shouldPassCorrectQueryToSearchEngine() {
    // Given
    String query = "brand intelligence search";
    int maxResults = 5;

    ArgumentCaptor<WebSearchRequest> requestCaptor = ArgumentCaptor.forClass(
        WebSearchRequest.class);
    when(webSearchResults.results()).thenReturn(Collections.emptyList());
    when(webSearchEngine.search(requestCaptor.capture())).thenReturn(webSearchResults);

    // When
    webSearchCandidateDiscovery.find(query, maxResults);

    // Then
    WebSearchRequest capturedRequest = requestCaptor.getValue();
    assertEquals(query, capturedRequest.searchTerms());
  }

  @Test
  void find_shouldThrowException_whenQueryIsNull() {
    // Given
    String query = null;
    int maxResults = 5;

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> webSearchCandidateDiscovery.find(query, maxResults));

    assertEquals("searchTerms cannot be null or blank", exception.getMessage());
  }

  @Test
  void find_shouldThrowException_whenQueryIsEmpty() {
    // Given
    String query = "";
    int maxResults = 5;

    // When & Then
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> webSearchCandidateDiscovery.find(query, maxResults));

    assertEquals("searchTerms cannot be null or blank", exception.getMessage());
  }

  @Test
  void find_shouldPropagateExceptionFromSearchEngine() {
    // Given
    String query = "test query";
    int maxResults = 5;
    RuntimeException expectedException = new RuntimeException("Search engine error");

    when(webSearchEngine.search(any(WebSearchRequest.class))).thenThrow(expectedException);

    // When & Then
    RuntimeException actualException = assertThrows(RuntimeException.class,
        () -> webSearchCandidateDiscovery.find(query, maxResults));

    assertEquals("Search engine error", actualException.getMessage());
  }

  private WebSearchOrganicResult createMockResult(String url) {
    WebSearchOrganicResult mockResult = mock(WebSearchOrganicResult.class);
    when(mockResult.url()).thenReturn(URI.create(url));
    return mockResult;
  }
}