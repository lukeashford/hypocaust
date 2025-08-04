package com.example.tool.extract;

import java.util.Optional;

/**
 * Interface for extracting readable content from HTML.
 */
public interface ContentExtractor {

  /**
   * Extract readable content from HTML.
   *
   * @param html the HTML content to extract from
   * @return the extracted content, or empty if extraction failed
   */
  Optional<PageContent> extract(String html);

}