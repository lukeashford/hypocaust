package com.example.the_machine.retriever.extract;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Composite content extractor that chains multiple extractors. Tries each extractor in order until
 * one succeeds.
 */
@Slf4j
@RequiredArgsConstructor
public class CompositeContentExtractor implements ContentExtractor {

  private final List<ContentExtractor> chain;

  @Override
  public Optional<PageContent> extract(String html) {
    if (html == null || html.isBlank()) {
      log.debug("[BoilerpipeContentExtractor] Extraction failed: empty html");
      return Optional.empty();
    }

    for (ContentExtractor extractor : chain) {
      Optional<PageContent> result = extractor.extract(html);
      if (result.isPresent()) {
        return result;
      }
    }

    return Optional.empty();
  }
}