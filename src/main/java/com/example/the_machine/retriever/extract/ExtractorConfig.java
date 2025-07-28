package com.example.the_machine.retriever.extract;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for content extractors.
 */
@Configuration
public class ExtractorConfig {

  @Bean
  @ConditionalOnProperty(name = "app.extractor", havingValue = "readability")
  public ContentExtractor readability() {
    return new ReadabilityContentExtractor();
  }

  @Bean
  @ConditionalOnProperty(name = "app.extractor", havingValue = "boilerpipe")
  public ContentExtractor boilerpipe() {
    return new BoilerpipeContentExtractor(BoilerpipeContentExtractor.Mode.ARTICLE);
  }

  @Bean
  @ConditionalOnProperty(name = "app.extractor", havingValue = "composite")
  public ContentExtractor contentExtractor() {
    return new CompositeContentExtractor(List.of(
        new ReadabilityContentExtractor(),
        new BoilerpipeContentExtractor(BoilerpipeContentExtractor.Mode.ARTICLE)
    ));
  }
}