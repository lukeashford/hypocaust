package com.example.tool.extract;

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
}