package com.example.the_machine.retriever.extract;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.dankito.readability4j.extended.Readability4JExtended;

/**
 * Readability-based content extractor using readability4j.
 */
@Slf4j
public class ReadabilityContentExtractor implements ContentExtractor {

  @Override
  public Optional<PageContent> extract(String html) {
    if (html == null || html.isBlank()) {
      log.debug("[BoilerpipeContentExtractor] Extraction failed: empty html");
      return Optional.empty();
    }

    try {
      val readability = new Readability4JExtended("", html);
      val article = readability.parse();

      if (article.getContent() != null && !article.getContent().isBlank()) {
        val title = article.getTitle() != null ? article.getTitle() : "";
        val text =
            article.getTextContent() != null ? article.getTextContent() : article.getContent();
        val excerpt = article.getExcerpt() != null ? article.getExcerpt() : "";

        log.debug("[ReadabilityContentExtractor] Successfully extracted content");
        return Optional.of(new PageContent(title, text, excerpt));
      }
    } catch (Exception e) {
      log.debug("[ReadabilityContentExtractor] Extraction failed: {}", e.getMessage());
    }

    return Optional.empty();
  }
}