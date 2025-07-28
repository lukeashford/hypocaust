package com.example.the_machine.retriever.extract;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.DefaultExtractor;
import de.l3s.boilerpipe.extractors.LargestContentExtractor;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * Extracts main text with Boilerpipe.
 *
 * <p>Usage: new BoilerpipeContentExtractor()  // uses ARTICLE mode
 * new BoilerpipeContentExtractor(Mode.LARGEST_CONTENT)</p>
 */
@Slf4j
@RequiredArgsConstructor
public class BoilerpipeContentExtractor implements ContentExtractor {

  public enum Mode {ARTICLE, LARGEST_CONTENT, DEFAULT}

  private final Mode mode;

  @Override
  public Optional<PageContent> extract(String html) {
    if (html == null || html.isBlank()) {
      log.debug("[BoilerpipeExtractor] ({}) empty html", mode);
      return Optional.empty();
    }

    try {
      val text = switch (mode) {
        case ARTICLE -> ArticleExtractor.INSTANCE.getText(html);
        case LARGEST_CONTENT -> LargestContentExtractor.INSTANCE.getText(html);
        case DEFAULT -> DefaultExtractor.INSTANCE.getText(html);
      };
      if (text != null && !text.isBlank()) {
        log.info("[BoilerpipeExtractor] ({}) success, {} chars", mode, text.length());
        return Optional.of(new PageContent("", text, ""));
      }
    } catch (BoilerpipeProcessingException e) {
      log.debug("[BoilerpipeExtractor] ({}) failed: {}", mode, e.getMessage());
    }

    return Optional.empty();
  }
}