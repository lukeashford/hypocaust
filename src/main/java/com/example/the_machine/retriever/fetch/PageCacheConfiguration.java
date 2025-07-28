package com.example.the_machine.retriever.fetch;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for PageFetcher cache.
 */
@Configuration
public class PageCacheConfiguration {

  /**
   * Cache bean for PageFetcher.
   */
  @Bean
  public Cache<URI, PageFetcher.Page> pageCache() {
    return Caffeine.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();
  }
}